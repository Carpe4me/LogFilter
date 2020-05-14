package com.legendmohe.tool.parser;

import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.T;
import com.legendmohe.tool.logtable.model.LogFilterTableModel;

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
public class BigoDevLogParser extends AbstractLogParser {

    private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter TIMESTAMP_DATE_TIME_FORMAT_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /*
    Match 1
    Full match	0-79	`[26][WARN][11-07 10:41:08.426(160985949)]SplashFragment:## navigate to MainPage`
    Group 1.	1-3	`26`
    Group 2.	5-9	`WARN`
    Group 3.	11-29	`11-07 10:41:08.426`
    Group 4.	30-39	`160985949`
    Group 5.	41-79	`SplashFragment:## navigate to MainPage`
     */
    final private Pattern mLogPattern = Pattern.compile("\\[(.+?)\\]\\[(\\w+)\\]\\[(.+?)\\((?:.+?)\\)\\](.+?):(.+)");

    @Override
    public LogInfo parseLog(String strText) {
        LogInfo logInfo = new LogInfo();
        try {
            Matcher matcher = mLogPattern.matcher(strText);
            if (matcher.find()) {
                String line = matcher.group(1);
                String level = matcher.group(2);
                String date = matcher.group(3);
                String tag = matcher.group(4);
                String msg = matcher.group(5);

                logInfo.setLine(Integer.valueOf(line));
                logInfo.setTime(date);

                Date parseDate = TIMESTAMP_FORMAT.parse(logInfo.getTime());
                parseDate.setYear(Calendar.getInstance().get(Calendar.YEAR) - 1900);
                logInfo.setTimestamp(parseDate.getTime());
                logInfo.setLogLV(level);
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

    @Override
    public int[] getSupportedColumns() {
        return new int[]{
                LogFilterTableModel.COLUMN_LINE,
                LogFilterTableModel.COLUMN_BOOKMARK,
                LogFilterTableModel.COLUMN_FILE,
                LogFilterTableModel.COLUMN_TIME,
                LogFilterTableModel.COLUMN_DATE,
                LogFilterTableModel.COLUMN_LOGLV,
                LogFilterTableModel.COLUMN_TAG,
                LogFilterTableModel.COLUMN_MESSAGE,
        };
    }
}
