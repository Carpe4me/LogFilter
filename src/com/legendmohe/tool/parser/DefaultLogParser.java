package com.legendmohe.tool.parser;

import com.legendmohe.tool.LogInfo;

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
}
