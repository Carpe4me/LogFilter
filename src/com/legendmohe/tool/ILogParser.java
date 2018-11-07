package com.legendmohe.tool;

import java.awt.*;

/**
 * 负责逐行解析log
 * 必须是无状态实现
 */
public interface ILogParser {

    LogInfo parseLog(String strText);

    Color getColor(LogInfo logInfo);

    int getLogLV(LogInfo logInfo);

    void loadProcessorFromConfig();
}
