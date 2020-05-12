package com.legendmohe.tool.config;

import java.awt.Color;

import javax.swing.UIManager;

public class ThemeConstant {
    ///////////////////////////////////颜色///////////////////////////////////

    public static String COLOR_HIGH_LIGHT_TYPE_FILTER = "FF0000";
    public static String COLOR_HIGH_LIGHT_TYPE_HIGH_LIGHT = "00FF00";
    public static String COLOR_HIGH_LIGHT_TYPE_SEARCH = "FFFF00";

    public static Color COLOR_TABLE_BG = UIManager.getColor("Table.background");
    public static Color COLOR_TABLE_SELECTION_BG = UIManager.getColor("Label.background");
    public static Color COLOR_TABLE_LOG_FLOW_SELECTION_BG = COLOR_TABLE_SELECTION_BG;

    public static Color COLOR_LOG_TABLE_CELL_BORDER = COLOR_TABLE_SELECTION_BG.darker();
    public static Color COLOR_FIX_POPUP_BACKGROUND = UIManager.getColor("Label.background");
    public static Color COLOR_FIX_POPUP_BUTTON_BG = UIManager.getColor("Label.foreground");
    public static Color COLOR_FIX_POPUP_BTN_PINNED = COLOR_FIX_POPUP_BUTTON_BG.brighter();
    public static Color COLOR_FIX_POPUP_BORDER = new Color(64, 131, 201);

    public static Color COLOR_INDICATOR_BOOKMARK = new Color(64, 131, 201);
    public static Color COLOR_INDICATOR_ERROR = new Color(232, 71, 0);

    //aarrggbb
    public static Color COLOR_LOG_TABLE_TEXT_DEFAULT = UIManager.getColor("Label.foreground");
    public static Color COLOR_LOG_TABLE_TEXT_DEBUG = COLOR_LOG_TABLE_TEXT_DEFAULT.darker();
    public static Color COLOR_LOG_TABLE_TEXT_ERROR = new Color(188, 63, 60);
    public static Color COLOR_LOG_TABLE_TEXT_FATAL = new Color(0x00FF0000);
    public static Color COLOR_LOG_TABLE_TEXT_INFO = new Color(98, 151, 85);
    public static Color COLOR_LOG_TABLE_TEXT_WARN = new Color(203, 119, 45);

    public static Color COLOR_LOG_TABLE_CELL_BG_BOOKMARK = COLOR_TABLE_SELECTION_BG.darker();
    public static Color COLOR_LOG_TABLE_CELL_BG_BOOKMARK_SELECTED = COLOR_TABLE_SELECTION_BG.darker();
    public static Color COLOR_LOG_TABLE_CELL_BG_NORMAL = new Color(0,0,0,0);

    public static Color COLOR_LOG_FLOW_FONT_NORMAL = COLOR_LOG_TABLE_TEXT_DEFAULT;
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
}
