package com.legendmohe.tool.parser;

import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.T;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hexinyu on 2018/11/7.
 */
public class IMODevLogParser extends AbstractLogParser {

    private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter TIMESTAMP_DATE_TIME_FORMAT_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /*
        Match 1
        Full match	0-80	03-24 18:09:31.763  2818  2818 D BatteryService: Sending ACTION_BATTERY_CHANGED.
        Group 1.	0-19	03-24 18:09:31.763
        Group 2.	20-24	2818
        Group 3.	26-30	2818
        Group 4.	31-32	D
        Group 5.	33-47	BatteryService
        Group 6.	48-80	 Sending ACTION_BATTERY_CHANGED.
     */
    final private Pattern mLogPattern = Pattern.compile("(.+)\\s+(\\d+)\\s+(\\d+)\\s+(\\w+)\\s+(.+?)\\s*:(.+)");

    @Override
    public LogInfo parseLog(String strText) {
        LogInfo logInfo = new LogInfo();
        try {
            Matcher matcher = mLogPattern.matcher(strText);
            if (matcher.find()) {
                String date = matcher.group(1);
                String pid = matcher.group(2);
                String tid = matcher.group(3);
                String level = matcher.group(4);
                String tag = matcher.group(5);
                String msg = matcher.group(6);

                logInfo.setTime(date);

                Date parseDate = TIMESTAMP_FORMAT.parse(logInfo.getTime());
                parseDate.setYear(Calendar.getInstance().get(Calendar.YEAR) - 1900);
                logInfo.setTimestamp(parseDate.getTime());
                logInfo.setLogLV(level);
                logInfo.setPid(pid);
                logInfo.setThread(tid);
                logInfo.setTag(tag);
                logInfo.setMessage(msg);
                logInfo.setTextColor(getFontColor(logInfo));
                logInfo.setDate(
                        LocalDateTime.ofInstant(Instant.ofEpochMilli(logInfo.getTimestamp()),
                                TimeZone.getDefault().toZoneId()).format(TIMESTAMP_DATE_TIME_FORMAT_LOCAL)); // 本地时间
            } else {
                logInfo.setMessage(strText);
            }
        } catch (Exception ex) {
            T.e(ex.getMessage());
            logInfo.setMessage(strText);
        }
        return logInfo;
    }
}
