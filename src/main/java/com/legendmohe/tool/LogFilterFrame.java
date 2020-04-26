package com.legendmohe.tool;

import com.legendmohe.tool.annotation.FieldSaveState;
import com.legendmohe.tool.annotation.UIStateSaver;
import com.legendmohe.tool.config.Constant;
import com.legendmohe.tool.view.AddTabComponent;
import com.legendmohe.tool.view.ButtonTabComponent;
import com.legendmohe.tool.view.RecentFileMenu;
import com.legendmohe.tool.view.TextConverterDialog;
import com.legendmohe.tool.view.XLogDecoderDialog;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
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
import java.util.Arrays;
import java.util.Map;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
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
        frameInfoProvider = new FrameInfoProvider() {
            @Override
            public JFrame getContainerFrame() {
                return LogFilterFrame.this;
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
        }));
        return component;
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
                    ((CloseableTab)componentAt).onCloseTab(index);
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
            setPreferredSize(new Dimension(m_nWinWidth, m_nWinHeight));
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

        toolsMenu.add(converterItem);
        toolsMenu.add(xlogDecoderItem);


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
        JFrame getContainerFrame();

        void onViewPortChanged(LogFilterComponent logFilterComponent, ChangeEvent e);

        void setTabTitle(LogFilterComponent filterComponent, String strTitle, String tips);

        boolean isFrameFocused();

        FloatingFrameInfo onFilterFloating(LogFilterComponent filter, Component component, String title);

        void beforeLogFileParse(String filename, LogFilterComponent filterComponent);
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
