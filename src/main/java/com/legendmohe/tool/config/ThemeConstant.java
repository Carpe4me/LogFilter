package com.legendmohe.tool.config;

import java.awt.Color;
import java.util.HashMap;
import java.util.Map;

import javax.swing.UIManager;

public class ThemeConstant {
    ///////////////////////////////////颜色///////////////////////////////////

    public static String COLOR_HIGH_LIGHT_TYPE_FILTER = "FF0000";
    public static String COLOR_HIGH_LIGHT_TYPE_HIGH_LIGHT = "00FF00";
    public static String COLOR_HIGH_LIGHT_TYPE_SEARCH = "FFFF00";

    public static Color COLOR_FIX_POPUP_BORDER = new Color(64, 131, 201);

    public static Color COLOR_INDICATOR_BOOKMARK = new Color(64, 131, 201);
    public static Color COLOR_INDICATOR_ERROR = new Color(232, 71, 0);

    //aarrggbb
    public static Color COLOR_LOG_TABLE_TEXT_ERROR = new Color(188, 63, 60);
    public static Color COLOR_LOG_TABLE_TEXT_FATAL = new Color(0x00FF0000);
    public static Color COLOR_LOG_TABLE_TEXT_INFO = new Color(98, 151, 85);
    public static Color COLOR_LOG_TABLE_TEXT_WARN = new Color(203, 119, 45);

    public static Color COLOR_LOG_FLOW_FONT_ERROR = COLOR_LOG_TABLE_TEXT_ERROR;
    public static Color COLOR_LOG_FLOW_NORMAL = new Color(0x0000ccff);
    public static Color COLOR_LOG_FLOW_NORMAL_LINE = new Color(0x00cce6ff);
    public static Color COLOR_LOG_FLOW_ERROR = new Color(0x00FF0000);
    public static Color COLOR_LOG_FLOW_ERROR_LINE = new Color(0x00ffb3b3);

    ///////////////////////////////////style///////////////////////////////////

    public static int TABLE_POPUP_BORDER_THICKNESS = 1;
    public static int LOG_TABLE_ROW_PADDING = 9;
    public static int LOG_TABLE_CELL_CONTENT_PADDING_LEFT = 4;
    public static int LOG_TABLE_CELL_CONTENT_PADDING_RIGHT = 8;

    public static final String THEME_NAME_DEFAULT = "default";
    public static final String THEME_NAME_DARK = "dark";
    public static final String THEME_NAME_LIGHT = "light";

    public static final Map<String, String> themeSettings = new HashMap<>();

    static {
        themeSettings.put(THEME_NAME_DEFAULT, "");
        themeSettings.put(THEME_NAME_LIGHT, "com.formdev.flatlaf.FlatLightLaf");
        themeSettings.put(THEME_NAME_DARK, "com.formdev.flatlaf.FlatDarculaLaf");
    }

    //////////////////////////////////////////////////////////////////////

    public static Color getColorTableBg() {
        return UIManager.getColor("Table.background");
    }

    public static Color getColorTableSelectionBg() {
        Color color = UIManager.getColor("Table.selectionBackground");
        return new Color(
                color.getRed(),
                color.getGreen(),
                color.getBlue(),
                0x3a
        );
//        return UIManager.getColor("Label.background");
    }

    public static Color getColorFixPopupBackground() {
        return UIManager.getColor("Label.background");
    }

    public static Color getColorFixPopupButtonBg() {
        return UIManager.getColor("Label.foreground");
    }

    public static Color getColorLogTableTextDefault() {
        return UIManager.getColor("Label.foreground");
    }

    public static Color getColorLogTableCellBorder() {
        return getColorTableSelectionBg().darker();
    }

    public static Color getColorFixPopupBtnPinned() {
        return getColorFixPopupButtonBg().brighter();
    }

    public static Color getColorLogTableTextDebug() {
        return getColorLogTableTextDefault().darker();
    }

    public static Color getColorLogTableCellBgBookmark() {
        return getColorTableSelectionBg().darker();
    }

    public static Color getColorLogTableCellBgBookmarkSelected() {
        return getColorTableSelectionBg().darker().darker();
    }

    public static Color getColorTableLogFlowSelectionBg() {
        return getColorTableSelectionBg();
    }

    public static Color getColorLogTableCellBgNormal() {
        return new Color(0,0,0,0);
    }

    public static Color getColorLogFlowFontNormal() {
        return getColorLogTableTextDefault();
    }
}
