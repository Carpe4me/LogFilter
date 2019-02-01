package com.legendmohe.tool.parser;

import com.legendmohe.tool.LogInfo;

import java.awt.Color;

/**
 * 负责逐行解析log
 * 必须是无状态实现
 */
public interface ILogParser {

    LogInfo parseLog(String strText);

    Color getColor(LogInfo logInfo);

    void loadProcessorFromConfig();
}
