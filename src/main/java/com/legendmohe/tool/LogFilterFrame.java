package com.legendmohe.tool;

import com.legendmohe.tool.annotation.FieldSaveState;
import com.legendmohe.tool.annotation.UIStateSaver;
import com.legendmohe.tool.config.Constant;
import com.legendmohe.tool.util.T;
import com.legendmohe.tool.util.Utils;
import com.legendmohe.tool.view.AddTabComponent;
import com.legendmohe.tool.view.AppSettingsDialog;
import com.legendmohe.tool.view.ButtonTabComponent;
import com.legendmohe.tool.view.RecentFileMenu;
import com.legendmohe.tool.view.TextContentDialog;
import com.legendmohe.tool.view.TextConverterDialog;
import com.legendmohe.tool.view.XLogDecoderDialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.nio.file.Paths;
import java.util.Arrays;

import javax.swing.AbstractAction;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTabbedPane;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;

public class LogFilterFrame extends JFrame {

    private FrameInfoProvider frameInfoProvider;
    private UIStateSaver mUIStateSaver;
    private FloatingWinListener floatingWinListener;

    @FieldSaveState
    int m_nWinWidth = Constant.DEFAULT_WIDTH;
    @FieldSaveState
    int m_nWinHeight = Constant.DEFAULT_HEIGHT;
    int m_nLastWidth;
    int m_nLastHeight;
    @FieldSaveState
    int mWindowState;
    @FieldSaveState
    String m_xLogDecoderPath = "";
    private JTabbedPane tabbedPane;

    ///////////////////////////////////init///////////////////////////////////

    public LogFilterFrame(FloatingWinListener floatingWinListener) throws HeadlessException {
        this.floatingWinListener = floatingWinListener;
        frameInfoProvider = new FrameInfoProviderAdapter() {
            @Override
            public Frame getContainerFrame() {
                return LogFilterFrame.this;
            }

            @Override
            public boolean enableFloatingWindow() {
                return true;
            }

            @Override
            public boolean enableLogFlow() {
                return true;
            }

            @Override
            public void onViewPortChanged(LogFilterComponent logFilterComponent, ChangeEvent e) {
                LogFilterFrame.this.onViewPortChanged(logFilterComponent, e);
            }

            @Override
            public void setTabTitle(LogFilterComponent filterComponent, String strTitle, String tips) {
                int i = tabbedPane.indexOfComponent(filterComponent);
                tabbedPane.setTitleAt(i, strTitle);
                tabbedPane.setToolTipTextAt(i, tips);
                SwingUtilities.invokeLater(() -> tabbedPane.getTabComponentAt(i).invalidate());
            }

            @Override
            public boolean isFrameFocused() {
                return LogFilterFrame.this.isFocused();
            }

            @Override
            public FloatingFrameInfo onFilterFloating(LogFilterComponent filter, Component component, String title) {
                if (LogFilterFrame.this.floatingWinListener != null) {
                    return LogFilterFrame.this.floatingWinListener.onQueryFloatingWin(component, title);
                }
                return null;
            }

            @Override
            public void beforeLogFileParse(String filename, LogFilterComponent filterComponent) {
                mRecentMenu.addEntry(filename);
            }
        };
        File outputDir = new File(Paths.get(frameInfoProvider.getProjectRootPath(), Constant.OUTPUT_LOG_DIR).toString());
        if (!outputDir.exists()) {
            outputDir.mkdirs();
            T.d("create log directory: " + outputDir.getAbsolutePath());
        }
        initUI();
        restoreUIState();
    }

    private void initUI() {
        setTitle(Constant.WINDOW_TITLE + " " + Constant.VERSION);
        JMenuBar menuBar = createMenuBar();
        setJMenuBar(menuBar);

        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());
        initTabPane(pane);

        addWindowListener(new WindowAdapter() {
            public void windowClosing(WindowEvent e) {
                exit();
            }
        });
        addWindowStateListener(new WindowStateListener() {
            @Override
            public void windowStateChanged(WindowEvent e) {
                mWindowState = e.getNewState();
            }
        });
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                withAllFilter(LogFilterComponent::restoreSplitPane);
            }
        });
    }

    private void initTabPane(Container pane) {
        tabbedPane = new JTabbedPane();
        addLogFilterComponentToTab(0);

        tabbedPane.addTab("Add", null, new JLabel(), "Add new log tab");
        tabbedPane.setTabComponentAt(1, new AddTabComponent(tabbedPane, new AddTabComponent.Listener() {
            @Override
            public void onAddButtonClicked(int addComponentIndex) {
                handleTabAddClicked(addComponentIndex);
            }
        }));
        tabbedPane.setEnabledAt(1, false);

        pane.add(tabbedPane, BorderLayout.CENTER);
        tabbedPane.setTabLayoutPolicy(JTabbedPane.SCROLL_TAB_LAYOUT);
    }

    private LogFilterComponent addLogFilterComponentToTab(int index) {
        LogFilterComponent component = new LogFilterComponent(frameInfoProvider);
        tabbedPane.insertTab("Log", null, component,
                "Open files or run logcat", index);
        tabbedPane.setTabComponentAt(index, new ButtonTabComponent(tabbedPane, new ButtonTabComponent.Listener() {
            @Override
            public void onCloseClicked(int index) {
                handleCloseTabClicked(index);
            }

            @Override
            public void onRightButtonClick(int index, int x, int y) {
                handleRightButtonClick(index, x, y);
            }
        }));
        return component;
    }

    private void handleRightButtonClick(int index, int x, int y) {
        JPopupMenu popup = createRightClickPopUp(index);
        if (popup != null) {
            popup.show(tabbedPane.getTabComponentAt(index), x, y);
        }
    }

    private JPopupMenu createRightClickPopUp(int index) {
        JPopupMenu menuPopup = new JPopupMenu();
        LogFilterComponent component = (LogFilterComponent) tabbedPane.getComponentAt(index);
        File[] files = component.getLastParseredFiles();
        if (files == null || files.length <= 0) {
            return null;
        }

        JMenuItem openPath = new JMenuItem(new AbstractAction("Open in explorer") {
            @Override
            public void actionPerformed(ActionEvent e) {
                Utils.openInExplorer(files[0]);
            }
        });
        JMenuItem copyPath = new JMenuItem(new AbstractAction("Copy full path to clipboard") {
            @Override
            public void actionPerformed(ActionEvent e) {
                File targetFile = files[0];
                Utils.sendContentToClipboard(targetFile.getAbsolutePath());
            }
        });

        menuPopup.add(openPath);
        menuPopup.add(copyPath);
        return menuPopup;
    }

    private void handleTabAddClicked(int addComponentIndex) {
        SwingUtilities.invokeLater(() -> {
            addLogFilterComponentToTab(addComponentIndex);
            tabbedPane.setSelectedIndex(addComponentIndex);
            withAllFilter(LogFilterComponent::restoreSplitPane);
        });
    }

    private void handleCloseTabClicked(int index) {
        if (index != -1) {
            // 最后一个不能关
            int tabCount = tabbedPane.getTabCount();
            if (tabCount > 2) {
                Component componentAt = tabbedPane.getComponentAt(index);
                if (componentAt instanceof CloseableTab) {
                    ((CloseableTab) componentAt).onCloseTab(index);
                }
                tabbedPane.remove(index);
                if (index == tabCount - 2) {
                    tabbedPane.setSelectedIndex(index - 1);
                } else {
                    tabbedPane.setSelectedIndex(index);
                }
            }
        }
    }

    private void withAllFilter(FilterLooper looper) {
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component component = tabbedPane.getComponentAt(i);
            if (looper != null && component instanceof LogFilterComponent) {
                looper.onLoop(((LogFilterComponent) component));
            }
        }
    }

    private void withCurrentFilter(FilterLooper looper) {
        Component component = tabbedPane.getSelectedComponent();
        if (component instanceof LogFilterComponent) {
            if (looper != null) {
                looper.onLoop(((LogFilterComponent) component));
            }
        }
    }

    private void withNewFilter(FilterLooper looper) {
        int tabCount = tabbedPane.getTabCount();
        LogFilterComponent newFilter = addLogFilterComponentToTab(tabCount - 1);
        tabbedPane.setSelectedIndex(tabCount - 1);
        withAllFilter(LogFilterComponent::restoreSplitPane);
        if (looper != null) {
            looper.onLoop(newFilter);
        }
    }

    LogFilterComponent getFocusedFilterComponent() {
        Component component = tabbedPane.getSelectedComponent();
        if (component instanceof LogFilterComponent) {
            return (LogFilterComponent) component;
        }
        return null;
    }

    private void restoreUIState() {
        // register state saver
        mUIStateSaver = new UIStateSaver(this, Constant.INI_FILE_STATE_MAIN_FRAME);
        mUIStateSaver.load();
        // restore
        SwingUtilities.invokeLater(() -> {
            withAllFilter(LogFilterComponent::restoreSplitPane);
            setMinimumSize(new Dimension(Constant.MIN_WIDTH, Constant.MIN_HEIGHT));
            setExtendedState(mWindowState);
            setSize(new Dimension(m_nWinWidth, m_nWinHeight));
        });
    }

    ///////////////////////////////////private///////////////////////////////////

    private void exit() {
        m_nWinWidth = getSize().width;
        m_nWinHeight = getSize().height;
        mUIStateSaver.save();
        withAllFilter(LogFilterComponent::exit);
        System.exit(0);
    }


    /////////////////////////////////public/////////////////////////////////////

    public void onViewPortChanged(LogFilterComponent logFilterComponent, ChangeEvent e) {
        if (getExtendedState() != JFrame.MAXIMIZED_BOTH) {
            m_nLastWidth = getWidth();
            m_nLastHeight = getHeight();
        }
    }

    public void parseLogFile(File[] files) {
        withCurrentFilter(filterComponent -> filterComponent.parseLogFile(files));
    }

    ///////////////////////////////////menu///////////////////////////////////

    private RecentFileMenu mRecentMenu;

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("File");
        fileMenu.setMnemonic(KeyEvent.VK_F);

        JMenuItem fileOpen = new JMenuItem("Open");
        fileOpen.setMnemonic(KeyEvent.VK_O);
        fileOpen.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_O,
                ActionEvent.ALT_MASK));
        fileOpen.setToolTipText("Open log file");
        fileOpen.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                withCurrentFilter(curFilter -> {
                    if (curFilter.hasLoadLogFileOrRunLogcat()) {
                        withNewFilter(filter -> filter.openFileBrowserToLoad(LogFilterComponent.FileType.LOG));
                    } else {
                        curFilter.openFileBrowserToLoad(LogFilterComponent.FileType.LOG);
                    }
                });
            }
        });

        mRecentMenu = new RecentFileMenu("RecentFile", 10) {
            public void onSelectFile(String filePath) {
                String[] files = filePath.split("\\|");
                File[] recentFiles = new File[files.length];
                for (int i = 0; i < files.length; i++) {
                    recentFiles[i] = new File(files[i]);
                }
                loadLogFiles(recentFiles);
            }
        };

        fileMenu.add(fileOpen);
        fileMenu.add(mRecentMenu);

        JMenu toolsMenu = new JMenu("Extra");

        JMenuItem converterItem = new JMenuItem("text converter");
        converterItem.setToolTipText("convert all kinds of log msg");
        converterItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                new TextConverterDialog().show();
            }
        });

        JMenuItem xlogDecoderItem = new JMenuItem("xlog decoder");
        xlogDecoderItem.setToolTipText("decode xlog files");
        xlogDecoderItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent event) {
                new XLogDecoderDialog(
                        m_xLogDecoderPath,
                        new XLogDecoderDialog.Listener() {
                            @Override
                            public void onClose(String decoderPath) {
                                m_xLogDecoderPath = decoderPath;
                            }

                            @Override
                            public void onLogDecoded(File[] decodedFiles) {
                                if (decodedFiles != null && decodedFiles.length > 0) {
                                    loadLogFiles(decodedFiles);
                                }
                            }
                        }
                ).show();
            }
        });

        JMenuItem logItem = new JMenuItem("Application Log");
        logItem.setToolTipText("Show LogFilter Logs");
        logItem.addActionListener(event -> new TextContentDialog(
                LogFilterFrame.this, "Log", T.getLogBuffer()
        ).show());

        JMenuItem settingsItem = new JMenuItem("Settings");
        settingsItem.addActionListener(event -> new AppSettingsDialog(LogFilterFrame.this).show());

        toolsMenu.add(converterItem);
        toolsMenu.add(xlogDecoderItem);
        toolsMenu.add(logItem);
        toolsMenu.add(settingsItem);


        menuBar.add(fileMenu);
        menuBar.add(toolsMenu);
        return menuBar;
    }

    private void loadLogFiles(File[] recentFiles) {
        T.d("loadlogFiles=" + Arrays.toString(recentFiles));
        withCurrentFilter(curFilter -> {
            if (curFilter.hasLoadLogFileOrRunLogcat()) {
                withNewFilter(filter -> filter.parseLogFile(recentFiles));
            } else {
                curFilter.parseLogFile(recentFiles);
            }
        });
    }

    //////////////////////////////////////////////////////////////////////

    public interface FrameInfoProvider {
        Frame getContainerFrame();

        boolean enableLogFlow();

        boolean enableFloatingWindow();

        void onViewPortChanged(LogFilterComponent logFilterComponent, ChangeEvent e);

        void setTabTitle(LogFilterComponent filterComponent, String strTitle, String tips);

        boolean isFrameFocused();

        FloatingFrameInfo onFilterFloating(LogFilterComponent filter, Component component, String title);

        void beforeLogFileParse(String filename, LogFilterComponent filterComponent);

        String getProjectRootPath();
    }

    public static class FrameInfoProviderAdapter implements FrameInfoProvider{

        @Override
        public Frame getContainerFrame() {
            return null;
        }

        @Override
        public boolean enableFloatingWindow() {
            return false;
        }

        public boolean enableLogFlow() {
            return false;
        }

        @Override
        public void onViewPortChanged(LogFilterComponent logFilterComponent, ChangeEvent e) {

        }

        @Override
        public void setTabTitle(LogFilterComponent filterComponent, String strTitle, String tips) {

        }

        @Override
        public boolean isFrameFocused() {
            return false;
        }

        @Override
        public FloatingFrameInfo onFilterFloating(LogFilterComponent filter, Component component, String title) {
            return null;
        }

        @Override
        public void beforeLogFileParse(String filename, LogFilterComponent filterComponent) {

        }

        @Override
        public String getProjectRootPath() {
            return "";
        }
    }

    private interface FilterLooper {
        void onLoop(LogFilterComponent filter);
    }

    interface CloseableTab {
        void onCloseTab(int index);
    }

    interface FloatingWinListener {
        FloatingFrameInfo onQueryFloatingWin(Component component, String title);
    }
}
