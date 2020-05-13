package com.legendmohe.tool.config;

import java.awt.Color;
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
    public static final int PARSER_TYPE_IOS_DEV_LOG = 5;
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

}
