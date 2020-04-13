package com.legendmohe.tool;

import com.legendmohe.tool.annotation.FieldSaveState;
import com.legendmohe.tool.annotation.UIStateSaver;
import com.legendmohe.tool.config.Constant;
import com.legendmohe.tool.view.AddTabComponent;
import com.legendmohe.tool.view.ButtonTabComponent;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.HeadlessException;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowStateListener;
import java.io.File;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTabbedPane;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;

public class LogFilterFrame extends JFrame {

    private FrameInfoProvider frameInfoProvider;
    private UIStateSaver mUIStateSaver;

    @FieldSaveState
    int m_nWinWidth = Constant.DEFAULT_WIDTH;
    @FieldSaveState
    int m_nWinHeight = Constant.DEFAULT_HEIGHT;
    int m_nLastWidth;
    int m_nLastHeight;
    @FieldSaveState
    int mWindowState;
    private JTabbedPane tabbedPane;

    ///////////////////////////////////init///////////////////////////////////

    public LogFilterFrame() throws HeadlessException {
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
            public void setTitle(LogFilterComponent filterComponent, String strTitle, String tips) {
                int i = tabbedPane.indexOfComponent(filterComponent);
                tabbedPane.setTitleAt(i, strTitle);
                tabbedPane.setToolTipTextAt(i, tips);
            }

            @Override
            public boolean isFocused() {
                return LogFilterFrame.this.isFocused();
            }
        };
        initUI();
        restoreUIState();
    }

    private void initUI() {
        setTitle(Constant.WINDOW_TITLE + " " + Constant.VERSION);

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

    private void addLogFilterComponentToTab(int index) {
        tabbedPane.insertTab("Log", null, new LogFilterComponent(frameInfoProvider),
                "Open files or run logcat", index);
        tabbedPane.setTabComponentAt(index, new ButtonTabComponent(tabbedPane, new ButtonTabComponent.Listener() {
            @Override
            public void onCloseClicked(int index) {
                handleCloseTabClicked(index);
            }
        }));
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
            if (tabbedPane.getTabCount() > 2) {
                tabbedPane.remove(index);
                tabbedPane.setSelectedIndex(index - 1);
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
        for (int i = 0; i < tabbedPane.getTabCount(); i++) {
            Component component = tabbedPane.getComponentAt(i);
            if (component.hasFocus() && component instanceof LogFilterComponent) {
                if (looper != null) {
                    looper.onLoop(((LogFilterComponent) component));
                }
                return;
            }
        }
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

    //////////////////////////////////////////////////////////////////////

    public interface FrameInfoProvider {
        JFrame getContainerFrame();

        void onViewPortChanged(LogFilterComponent logFilterComponent, ChangeEvent e);

        void setTitle(LogFilterComponent filterComponent, String strTitle, String tips);

        boolean isFocused();
    }

    private interface FilterLooper {
        void onLoop(LogFilterComponent filter);
    }
}
