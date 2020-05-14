package com.legendmohe.tool.config;

import java.util.HashMap;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class AppSettings {

    public static final String KEY_THEME = "theme";

    private static final Map<String, String> settings = new HashMap<>();

    public static final String getTheme() {
        return settings.getOrDefault(KEY_THEME, "");
    }

    public static final boolean setTheme(String themeName) {
        if (themeName == null) {
            return false;
        }

        String oldValue = settings.get(KEY_THEME);
        settings.put(KEY_THEME, themeName);
        if (!themeName.equals(oldValue)) {
            return notifySettingsChanged(KEY_THEME, themeName);
        }
        return true;
    }

    private static boolean notifySettingsChanged(String key, String value) {
        switch (key) {
            case KEY_THEME: {
                return changeTheme(value);
            }
        }

        return false;
    }

    private static boolean changeTheme(String themeName) {
        String lnfName = ThemeConstant.themeSettings.getOrDefault(themeName, "");
        if (themeName.length() <= 0) {
            return false;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                // default
                if (lnfName.length() == 0) {
                    UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                } else {
                    UIManager.setLookAndFeel(lnfName);
                }
//            SwingUtilities.updateComponentTreeUI(frame);
//            frame.pack();
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }
        });
        return true;
    }
}
