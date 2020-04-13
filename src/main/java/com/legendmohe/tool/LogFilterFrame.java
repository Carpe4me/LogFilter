package com.legendmohe.tool;

import com.legendmohe.tool.annotation.FieldSaveState;
import com.legendmohe.tool.annotation.UIStateSaver;
import com.legendmohe.tool.config.Constant;

import java.awt.BorderLayout;
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
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;

public class LogFilterFrame extends JFrame {

    private LogFilterComponent logFilterComponent;
    private FrameInfoProvider frameInfoProvider;
    private final UIStateSaver mUIStateSaver;

    @FieldSaveState
    int m_nWinWidth = Constant.DEFAULT_WIDTH;
    @FieldSaveState
    int m_nWinHeight = Constant.DEFAULT_HEIGHT;
    int m_nLastWidth;
    int m_nLastHeight;
    @FieldSaveState
    int mWindowState;

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
            public void setTitle(String strTitle) {
                LogFilterFrame.this.setTitle(strTitle);
            }

            @Override
            public boolean isFocused() {
                return LogFilterFrame.this.isFocused();
            }
        };
        logFilterComponent = new LogFilterComponent(frameInfoProvider);
        Container pane = getContentPane();
        pane.setLayout(new BorderLayout());
        pane.add(logFilterComponent, BorderLayout.CENTER);

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
                logFilterComponent.restoreSplitPane();
            }
        });
        logFilterComponent.restoreSplitPane();

        // register state saver
        mUIStateSaver = new UIStateSaver(this, Constant.INI_FILE_STATE_MAIN_FRAME);
        mUIStateSaver.load();
        // restore
        SwingUtilities.invokeLater(() -> {
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
        logFilterComponent.exit();
    }


    /////////////////////////////////public/////////////////////////////////////

    public void onViewPortChanged(LogFilterComponent logFilterComponent, ChangeEvent e) {
        if (getExtendedState() != JFrame.MAXIMIZED_BOTH) {
            m_nLastWidth = getWidth();
            m_nLastHeight = getHeight();
        }
    }

    public void parseLogFile(File[] files) {
        logFilterComponent.parseLogFile(files);
    }

    //////////////////////////////////////////////////////////////////////

    public interface FrameInfoProvider {
        JFrame getContainerFrame();

        void onViewPortChanged(LogFilterComponent logFilterComponent, ChangeEvent e);

        void setTitle(String strTitle);

        boolean isFocused();
    }
}
