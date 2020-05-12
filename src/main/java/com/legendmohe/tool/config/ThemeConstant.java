package com.legendmohe.tool.config;

import java.awt.Color;

public class ThemeConstant {
    ///////////////////////////////////颜色///////////////////////////////////

    public static Color COLOR_TABLE_SELECTION_BG = new Color(196, 196, 196);
    public static Color COLOR_TABLE_LOG_FLOW_SELECTION_BG = new Color(208, 226, 255);
    public static Color COLOR_INDICATOR_BOOKMARK = new Color(64, 131, 201);

    public static String COLOR_HIGH_LIGHT_TYPE_FILTER = "FF0000";
    public static String COLOR_HIGH_LIGHT_TYPE_HIGH_LIGHT = "00FF00";
    public static String COLOR_HIGH_LIGHT_TYPE_SEARCH = "FFFF00";

    public static Color COLOR_LOG_CELL_BORDER = new Color(0x00000000);
    public static Color COLOR_LOG_TABLE_POPUP_BACKGROUND = new Color(242, 242, 242);
    public static Color COLOR_LOG_TABLE_POPUP_BORDER = new Color(64, 131, 201);
    public static Color COLOR_FIX_POPUP_BUTTON_BG = Color.gray;
    public static Color COLOR_FIX_POPUP_BTN_PINNED = Color.DARK_GRAY;
    //aarrggbb
    public static Color COLOR_LOG_TABLE_TEXT_DEBUG = new Color(0x000000AA);
    public static Color COLOR_LOG_TABLE_TEXT_ERROR = new Color(232, 71, 0);
    public static Color COLOR_LOG_TABLE_TEXT_FATAL = new Color(0x00FF0000);
    public static Color COLOR_LOG_TABLE_TEXT_INFO = new Color(0x00009A00);
    public static Color COLOR_LOG_TABLE_TEXT_WARN = new Color(0xDE8700);
    public static Color COLOR_LOG_TABLE_CELL_BG_BOOKMARK = new Color(0xD9D9D9);
    public static Color COLOR_LOG_TABLE_CELL_BG_BOOKMARK_SELECTED = new Color(0x00DDDDFF);

    public static Color COLOR_LOG_TABLE_CELL_BG_NORMAL = Color.WHITE;
    public static Color COLOR_LOG_TABLE_TEXT_DEFAULT = Color.BLACK;

    public static Color COLOR_INDICATOR_ERROR = COLOR_LOG_TABLE_TEXT_ERROR;

    public static Color COLOR_LOG_FLOW_FONT_NORMAL = new Color(0x000000);
    public static Color COLOR_LOG_FLOW_FONT_ERROR = new Color(0x00FF0000);
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
