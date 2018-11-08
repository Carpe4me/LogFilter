package com.legendmohe.tool.parser;

import com.legendmohe.tool.LogColor;
import com.legendmohe.tool.LogInfo;

import java.awt.Color;

/**
 * Created by hexinyu on 2018/11/7.
 */
abstract class AbstractLogParser implements ILogParser {

    @Override
    public Color getColor(LogInfo logInfo) {
        if (logInfo.getLogLV() == null) return Color.BLACK;

        if (logInfo.getLogLV().equals("FATAL") || logInfo.getLogLV().equals("F"))
            return new Color(LogColor.COLOR_FATAL);
        if (logInfo.getLogLV().equals("ERROR") || logInfo.getLogLV().equals("E") || logInfo.getLogLV().equals("3"))
            return new Color(LogColor.COLOR_ERROR);
        else if (logInfo.getLogLV().equals("WARN") || logInfo.getLogLV().equals("W") || logInfo.getLogLV().equals("4"))
            return new Color(LogColor.COLOR_WARN);
        else if (logInfo.getLogLV().equals("INFO") || logInfo.getLogLV().equals("I") || logInfo.getLogLV().equals("6"))
            return new Color(LogColor.COLOR_INFO);
        else if (logInfo.getLogLV().equals("DEBUG") || logInfo.getLogLV().equals("D") || logInfo.getLogLV().equals("7"))
            return new Color(LogColor.COLOR_DEBUG);
        else if (logInfo.getLogLV().equals("0"))
            return new Color(LogColor.COLOR_0);
        else if (logInfo.getLogLV().equals("1"))
            return new Color(LogColor.COLOR_1);
        else if (logInfo.getLogLV().equals("2"))
            return new Color(LogColor.COLOR_2);
        else if (logInfo.getLogLV().equals("5"))
            return new Color(LogColor.COLOR_5);
        else
            return Color.BLACK;
    }

    @Override
    public int getLogLV(LogInfo logInfo) {
        if (logInfo.getLogLV() == null) return LogInfo.LOG_LV_VERBOSE;

        if (logInfo.getLogLV().equals("FATAL") || logInfo.getLogLV().equals("F"))
            return LogInfo.LOG_LV_FATAL;
        if (logInfo.getLogLV().equals("ERROR") || logInfo.getLogLV().equals("E"))
            return LogInfo.LOG_LV_ERROR;
        else if (logInfo.getLogLV().equals("WARN") || logInfo.getLogLV().equals("W"))
            return LogInfo.LOG_LV_WARN;
        else if (logInfo.getLogLV().equals("INFO") || logInfo.getLogLV().equals("I"))
            return LogInfo.LOG_LV_INFO;
        else if (logInfo.getLogLV().equals("DEBUG") || logInfo.getLogLV().equals("D"))
            return LogInfo.LOG_LV_DEBUG;
        else
            return LogInfo.LOG_LV_VERBOSE;
    }
}
