package com.legendmohe.tool.config;

import java.io.File;

public class Constant {

    public static final String WINDOW_TITLE = "LogFilter";
    public static final String VERSION = "Version 1.9.0";
    public static final String OUTPUT_LOG_DIR = "log";
    public static final String CONFIG_BASE_DIR = "conf";

    public static final int DEFAULT_WIDTH = 1200;
    public static final int DEFAULT_HEIGHT = 720;
    public static final int MIN_WIDTH = 1100;
    public static final int MIN_HEIGHT = 500;
    // 对应menu的index，必须从0开始
    public static final int PARSER_TYPE_DEFAULT_LOG = 0;
    public static final int PARSER_TYPE_LOGCAT = 1;
    public static final int PARSER_TYPE_BIGO_DEV_LOG = 2;
    public static final int PARSER_TYPE_BIGO_XLOG = 3;
    public static final int PARSER_TYPE_IMO_DEV_LOG = 4;
    static final int DEVICES_ANDROID = 0;
    static final int DEVICES_IOS = 1;
    static final int DEVICES_CUSTOM = 2;
    public static final int PARSING_STATUS_CHANGE_PENDING = 1;
    public static final int PARSING_STATUS_PARSING = 2;
    public static final int PARSING_STATUS_READY = 4;
    public static final String DIFF_PROGRAM_PATH = "C:\\Program Files\\KDiff3\\kdiff3.exe";

    public static final int EXT_DIALOG_TYPE_INCLUDE_TAG = 0;
    public static final int EXT_DIALOG_TYPE_EXCLUDE_TAG = 1;

    public static final String LOG_FLOW_CONFIG_DIR = "logflow";

    ///////////////////////////////////logcat command///////////////////////////////////

    public static final String COMBO_ANDROID = "Android";
    public static final String COMBO_CUSTOM_COMMAND = "custom command";
    public static final String ANDROID_DEFAULT_CMD = "logcat -v time";
    public static final String ANDROID_THREAD_CMD = "logcat -v threadtime";
    public static final String ANDROID_EVENT_CMD = "logcat -b events -v time";
    public static final String ANDROID_RADIO_CMD = "logcat -b radio -v time";
    public static final String ANDROID_CUSTOM_CMD = "shell cat /proc/kmsg";
    public static final String ANDROID_DEFAULT_CMD_FIRST = "adb ";
    public static final String ANDROID_SELECTED_CMD_FIRST = "adb -s ";
    public static final String[] DEVICES_CMD = {"adb devices -l", "", ""};

    ///////////////////////////////////ini 配置相关///////////////////////////////////

    public final static String INI_FILE_DUMP_SYS = CONFIG_BASE_DIR + File.separator + "LogFilterDumpsysCmd.ini";
    public final static String INI_FILE_STATE = CONFIG_BASE_DIR + File.separator + "LogFilterState.ser";
    public final static String INI_FILE_STATE_MAIN_FRAME = CONFIG_BASE_DIR + File.separator + "LogFilterState_mainframe.ser";
    public final static String INI_FILE_COLOR = CONFIG_BASE_DIR + File.separator + "LogFilterColor.ini";
    public final static String INI_FILE_CMD = CONFIG_BASE_DIR + File.separator + "LogFilterCmd.ini";

    public static final String INI_CMD_COUNT = "CMD_COUNT";
    public static final String INI_CMD = "CMD_";
    public static final String INI_COLOR_0 = "INI_COLOR_0";
    public static final String INI_COLOR_1 = "INI_COLOR_1";
    public static final String INI_COLOR_2 = "INI_COLOR_2";
    public static final String INI_COLOR_3 = "INI_COLOR_3(E)";
    public static final String INI_COLOR_4 = "INI_COLOR_4(W)";
    public static final String INI_COLOR_5 = "INI_COLOR_5";
    public static final String INI_COLOR_6 = "INI_COLOR_6(I)";
    public static final String INI_COLOR_7 = "INI_COLOR_7(D)";
    public static final String INI_COLOR_8 = "INI_COLOR_8(F)";
    public static final String INI_HIGILIGHT_COUNT = "INI_HIGILIGHT_COUNT";
    public static final String INI_HIGILIGHT_ = "INI_HIGILIGHT_";

    ///////////////////////////////////颜色///////////////////////////////////

    //aarrggbb
    public static int COLOR_GUIDE = 0x00000000;
    public static int COLOR_BOOKMARK = 0x00DDDDDD;
    public static int COLOR_BOOKMARK2 = 0x00DDDDFF;
    public static int COLOR_LOG_FLOW_NORMAL = 0x0000ccff;
    public static int COLOR_LOG_FLOW_NORMAL_LINE = 0x00cce6ff;
    public static int COLOR_LOG_FLOW_ERROR = 0x00FF0000;
    public static int COLOR_LOG_FLOW_ERROR_LINE = 0x00ffb3b3;
    public static int COLOR_LOG_FLOW_TEXT = 0x00484848;
    public static int COLOR_DEBUG = 0x000000AA;
    public static int COLOR_ERROR = 0x00FF0000;
    public static int COLOR_FATAL = 0x00FF0000;
    public static int COLOR_INFO = 0x00009A00;
    public static int COLOR_WARN = 0x00FF9A00;
    public static int COLOR_0 = 0x00000000;
    public static int COLOR_1 = 0x00000000;
    public static int COLOR_2 = 0x00000000;
    public static int COLOR_3 = COLOR_ERROR;
    public static int COLOR_4 = COLOR_WARN;
    public static int COLOR_5 = 0x00000000;
    public static int COLOR_6 = COLOR_INFO;
    public static int COLOR_7 = COLOR_DEBUG;
    public static int COLOR_8 = COLOR_ERROR;
    public static String[] COLOR_HIGHLIGHT;

    public static final String COLOR_HIGH_LIGHT_TYPE_FILTER = "FF0000";
    public static final String COLOR_HIGH_LIGHT_TYPE_HIGH_LIGHT = "00FF00";
    public static final String COLOR_HIGH_LIGHT_TYPE_SEARCH = "FFFF00";
    public static final int COLOR_LOG_CELL_BORDER = 0x00000000;
    public static final int COLOR_LOG_TABLE_POPUP_BACKGROUND = 0xDAD8E5;

    ///////////////////////////////////style///////////////////////////////////

    public static final int LOG_TABLE_ROW_PADDING = 9;
    public static final int LOG_TABLE_CELL_CONTENT_PADDING = 4;
}
