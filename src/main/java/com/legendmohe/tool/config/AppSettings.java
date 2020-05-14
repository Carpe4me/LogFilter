package com.legendmohe.tool.config;

import com.legendmohe.tool.util.T;

import java.awt.Window;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

public class AppSettings {

    public static final String KEY_THEME = "theme";

    private static final Map<String, String> settings = new HashMap<>();

    private static StoreHelper storeHelper;

    public static void setStoreHelper(StoreHelper storeHelper) {
        AppSettings.storeHelper = storeHelper;
    }

    //////////////////////////////////////////////////////////////////////

    public static String getTheme() {
        return settings.getOrDefault(KEY_THEME, "");
    }

    public static boolean setTheme(String themeName) {
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

    //////////////////////////////////////////////////////////////////////

    public static boolean notifySettingsChanged(String key, String value) {
        boolean success = false;
        switch (key) {
            case KEY_THEME: {
                success = changeTheme(value);
                break;
            }
        }
        if (success && storeHelper != null) {
            storeHelper.scheduleSave(settings);
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

                for (Window window : Window.getWindows()) {
                    SwingUtilities.updateComponentTreeUI(window);
                    window.pack();
                }
            } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException e) {
                e.printStackTrace();
            }
        });
        return true;
    }

    public static void install() {
        if (storeHelper != null) {
            storeHelper.install(settings);
        }
    }

    ///////////////////////////////////persistence///////////////////////////////////

    public interface StoreHelper {
        void scheduleSave(Map<String, String> data);

        void install(Map<String, String> data);
    }

    public static class DefaultStoreHelper implements StoreHelper {

        @Override
        public void scheduleSave(Map<String, String> data) {
            Properties p = new Properties();
            try {
                try {
                    p.load(new FileInputStream(Constant.INI_FILE_APP_SETTINGS));
                } catch (FileNotFoundException ex) {
                    T.d(Constant.INI_FILE_APP_SETTINGS + " not exist!");
                }
                for (String key : data.keySet()) {
                    p.setProperty(key, data.get(key));
                }
                p.store(new FileOutputStream(Constant.INI_FILE_APP_SETTINGS), null);
            } catch (FileNotFoundException e) {
                T.d(Constant.INI_FILE_APP_SETTINGS + " not exist!");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void install(Map<String, String> data) {
            Properties p = new Properties();
            try {
                p.load(new FileInputStream(Constant.INI_FILE_APP_SETTINGS));
            } catch (FileNotFoundException e) {
                T.d(Constant.INI_FILE_APP_SETTINGS + " not exist!");
                return;
            } catch (IOException e) {
                e.printStackTrace();
                return;
            }
            Enumeration names = p.propertyNames();
            while (names.hasMoreElements()) {
                String name = (String) names.nextElement();
                final String value = p.getProperty(name);
                if (value != null) {
                    data.put(name, value);
                }
            }
        }
    }
}
