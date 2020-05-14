package com.legendmohe.tool;

import com.legendmohe.tool.config.AppSettings;
import com.legendmohe.tool.thirdparty.util.OsCheck;

import java.awt.Component;
import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.text.DefaultEditorKit;

public class Main {

    ///////////////////////////////////main///////////////////////////////////

    public static void main(final String args[]) {
        // You should always work with UI inside Event Dispatch Thread (EDT)
        // That includes installing L&F, creating any Swing components etc.
        SwingUtilities.invokeLater(() -> {
            // load settings from file
            AppSettings.setStoreHelper(new AppSettings.DefaultStoreHelper());
            AppSettings.install();
            // setup platform spec
            configByPlatform();

            final LogFilterFrame main = new LogFilterFrame(new FloatingWinListener());
            main.pack();
            main.setVisible(true);

            if (args != null && args.length > 0) {
                EventQueue.invokeLater(() -> {
                    File[] files = new File[args.length];
                    for (int i = 0; i < args.length; i++) {
                        files[i] = new File(args[i]).getAbsoluteFile();
                    }
                    main.parseLogFile(files);
                });
            }
        });
    }

    private static void configByPlatform() {
        AppSettings.notifySettingsChanged(AppSettings.KEY_THEME, AppSettings.getTheme());

        if (OsCheck.getOperatingSystemType() == OsCheck.OSType.MacOS) {
            InputMap im = (InputMap) UIManager.get("TextField.focusInputMap");
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_C, KeyEvent.META_DOWN_MASK), DefaultEditorKit.copyAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_V, KeyEvent.META_DOWN_MASK), DefaultEditorKit.pasteAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_X, KeyEvent.META_DOWN_MASK), DefaultEditorKit.cutAction);
            im.put(KeyStroke.getKeyStroke(KeyEvent.VK_A, KeyEvent.META_DOWN_MASK), DefaultEditorKit.selectAllAction);
        }
    }

    public static void setUIFont(javax.swing.plaf.FontUIResource f) {
        java.util.Enumeration keys = UIManager.getDefaults().keys();
        while (keys.hasMoreElements()) {
            Object key = keys.nextElement();
            Object value = UIManager.get(key);
            if (value instanceof javax.swing.plaf.FontUIResource)
                UIManager.put(key, f);
        }
    }

    //////////////////////////////////////////////////////////////////////

    private static final class FloatingWinState {
        JComponent parent;
        Component component;
        int index;
        JFrame floatingFrame;

        public FloatingWinState(JComponent parent, Component component, int index, JFrame floatingFrame) {
            this.parent = parent;
            this.component = component;
            this.index = index;
            this.floatingFrame = floatingFrame;
        }
    }

    // 负责管理floating win
    private static class FloatingWinListener implements LogFilterFrame.FloatingWinListener {

        private Map<Component, FloatingWinState> mParentMap = new HashMap<>();

        @Override
        public FloatingFrameInfo onQueryFloatingWin(Component component, String title) {
            FloatingWinState state = mParentMap.get(component);
            if (state == null) {
                JComponent parent = (JComponent) component.getParent();

                int idx = -1;
                for (int i = 0; i < parent.getComponentCount(); i++) {
                    if (parent.getComponent(i) == component) {
                        idx = i;
                        break;
                    }
                }

                component.getParent().remove(idx);

                final int finalIdx = idx;
                JFrame newFloatingWin = new JFrame();
                newFloatingWin.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowClosing(WindowEvent e) {
                        parent.add(component, finalIdx);
                        mParentMap.remove(component);
                    }
                });
                newFloatingWin.setTitle(title);
                newFloatingWin.getContentPane().add(component);
                newFloatingWin.pack();
                newFloatingWin.setVisible(true);


                mParentMap.put(component, new FloatingWinState(
                        parent, component, idx, newFloatingWin
                ));
                return new FloatingFrameInfo(newFloatingWin, false);
            } else {
                state.floatingFrame.dispatchEvent(
                        new WindowEvent(state.floatingFrame, WindowEvent.WINDOW_CLOSING)
                );
                return new FloatingFrameInfo(state.floatingFrame, true);
            }
        }
    }
}
