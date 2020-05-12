package com.legendmohe.tool.parser;

import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.config.ThemeConstant;
import com.legendmohe.tool.logtable.model.LogFilterTableModel;

import java.awt.Color;

/**
 * Created by hexinyu on 2018/11/7.
 */
public abstract class AbstractLogParser implements ILogParser {

    @Override
    public Color getFontColor(LogInfo logInfo) {
        if (logInfo.getLogLV() == null) return ThemeConstant.COLOR_LOG_TABLE_TEXT_DEFAULT;

        if (logInfo.getLogLV().equals("FATAL") || logInfo.getLogLV().equals("F"))
            return ThemeConstant.COLOR_LOG_TABLE_TEXT_FATAL;
        if (logInfo.getLogLV().equals("ERROR") || logInfo.getLogLV().equals("E") || logInfo.getLogLV().equals("3"))
            return ThemeConstant.COLOR_LOG_TABLE_TEXT_ERROR;
        else if (logInfo.getLogLV().equals("WARN") || logInfo.getLogLV().equals("W") || logInfo.getLogLV().equals("4"))
            return ThemeConstant.COLOR_LOG_TABLE_TEXT_WARN;
        else if (logInfo.getLogLV().equals("INFO") || logInfo.getLogLV().equals("I") || logInfo.getLogLV().equals("6"))
            return ThemeConstant.COLOR_LOG_TABLE_TEXT_INFO;
        else if (logInfo.getLogLV().equals("DEBUG") || logInfo.getLogLV().equals("D") || logInfo.getLogLV().equals("7"))
            return ThemeConstant.COLOR_LOG_TABLE_TEXT_DEBUG;
        else
            return ThemeConstant.COLOR_LOG_TABLE_TEXT_DEFAULT;
    }

    public static final int[] gDefColumns = new int[LogFilterTableModel.COLUMN_MAX];

    static {
        for (int i = 0; i < gDefColumns.length; i++) {
            gDefColumns[i] = i;
        }
    }

    @Override
    public int[] getSupportedColumns() {
        return gDefColumns;
    }
}
