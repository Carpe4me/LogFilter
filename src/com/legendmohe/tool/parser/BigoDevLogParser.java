package com.legendmohe.tool.parser;

import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.T;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hexinyu on 2018/11/7.
 */
public class BigoDevLogParser extends AbstractLogParser {

    static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");

    /*
    Match 1
    Full match	0-79	`[26][WARN][11-07 10:41:08.426(160985949)]SplashFragment:## navigate to MainPage`
    Group 1.	1-3	`26`
    Group 2.	5-9	`WARN`
    Group 3.	11-29	`11-07 10:41:08.426`
    Group 4.	30-39	`160985949`
    Group 5.	41-79	`SplashFragment:## navigate to MainPage`
     */
    final private Pattern mLogPattern = Pattern.compile("\\[(\\d+)\\]\\[(\\w+)\\]\\[(.+?)\\((\\d+)\\)\\](.+?):(.+)");

    @Override
    public LogInfo parseLog(String strText) {
        LogInfo logInfo = new LogInfo();
        try {
            Matcher matcher = mLogPattern.matcher(strText);
            if (matcher.find()) {
                String line = matcher.group(1);
                String level = matcher.group(2);
                String date = matcher.group(3);
                String unknown = matcher.group(4);
                String tag = matcher.group(5);
                String msg = matcher.group(6);

                logInfo.setLine(Integer.valueOf(line));
                logInfo.setDate(date);
                logInfo.setTime(date);
                logInfo.setTimestamp(TIMESTAMP_FORMAT.parse(logInfo.getTime()).getTime());
                logInfo.setLogLV(level);
                logInfo.setTag(tag);
                logInfo.setMessage(msg);
                logInfo.setTextColor(getColor(logInfo));
            } else {
                logInfo.setMessage(strText);
            }
        } catch (Exception ex) {
            T.e(ex.getMessage());
            logInfo.setMessage(strText);
        }
        return logInfo;
    }

    @Override
    public void loadProcessorFromConfig() {
        // ignore
    }
}
