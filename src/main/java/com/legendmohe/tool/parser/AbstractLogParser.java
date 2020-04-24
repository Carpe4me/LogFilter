package com.legendmohe.tool.parser;

import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.config.Constant;
import com.legendmohe.tool.logtable.model.LogFilterTableModel;

import java.awt.Color;

/**
 * Created by hexinyu on 2018/11/7.
 */
public abstract class AbstractLogParser implements ILogParser {

    @Override
    public Color getFontColor(LogInfo logInfo) {
        if (logInfo.getLogLV() == null) return Color.BLACK;

        if (logInfo.getLogLV().equals("FATAL") || logInfo.getLogLV().equals("F"))
            return Constant.COLOR_FATAL;
        if (logInfo.getLogLV().equals("ERROR") || logInfo.getLogLV().equals("E") || logInfo.getLogLV().equals("3"))
            return Constant.COLOR_ERROR;
        else if (logInfo.getLogLV().equals("WARN") || logInfo.getLogLV().equals("W") || logInfo.getLogLV().equals("4"))
            return Constant.COLOR_WARN;
        else if (logInfo.getLogLV().equals("INFO") || logInfo.getLogLV().equals("I") || logInfo.getLogLV().equals("6"))
            return Constant.COLOR_INFO;
        else if (logInfo.getLogLV().equals("DEBUG") || logInfo.getLogLV().equals("D") || logInfo.getLogLV().equals("7"))
            return Constant.COLOR_DEBUG;
        else if (logInfo.getLogLV().equals("0"))
            return Constant.COLOR_0;
        else if (logInfo.getLogLV().equals("1"))
            return Constant.COLOR_1;
        else if (logInfo.getLogLV().equals("2"))
            return Constant.COLOR_2;
        else if (logInfo.getLogLV().equals("5"))
            return Constant.COLOR_5;
        else
            return Constant.COLOR_UNKNOWN;
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
