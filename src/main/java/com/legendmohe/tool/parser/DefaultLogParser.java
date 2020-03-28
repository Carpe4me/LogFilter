package com.legendmohe.tool.parser;

import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.logtable.model.LogFilterTableModel;

/**
 * do nothing
 * <p>
 * Created by hexinyu on 2018/11/7.
 */
public class DefaultLogParser extends AbstractLogParser {

    @Override
    public LogInfo parseLog(String strText) {
        LogInfo logInfo = new LogInfo();
        logInfo.setMessage(strText);
        return logInfo;
    }

    @Override
    public int[] getSupportedColumns() {
        return new int[] {
                LogFilterTableModel.COLUMN_LINE,
                LogFilterTableModel.COLUMN_BOOKMARK,
                LogFilterTableModel.COLUMN_MESSAGE
        };
    }
}
