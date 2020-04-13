package com.legendmohe.tool;

import com.legendmohe.tool.thirdparty.util.OsCheck;

import java.awt.EventQueue;
import java.awt.Font;
import java.awt.event.KeyEvent;
import java.io.File;

import javax.swing.InputMap;
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
            configByPlatform();

            final LogFilterFrame main = new LogFilterFrame();
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
        if (OsCheck.getOperatingSystemType() == OsCheck.OSType.Windows) {
            setUIFont(new javax.swing.plaf.FontUIResource("微软雅黑", Font.PLAIN, 12));
        } else {
            setUIFont(new javax.swing.plaf.FontUIResource("Consoles", Font.PLAIN, 12));
        }
        try {
            UIManager.setLookAndFeel(
                    UIManager.getCrossPlatformLookAndFeelClassName());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (UnsupportedLookAndFeelException e) {
            e.printStackTrace();
        }

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
}
