package com.legendmohe.tool.parser;

import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.T;
import com.legendmohe.tool.logtable.model.LogFilterTableModel;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hexinyu on 2018/11/7.
 */
public class BigoIOSDevLogParser extends AbstractLogParser {

    private static final DateFormat TIMESTAMP_FORMAT_WITH_TIMEZONE = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS Z");
    private static final DateTimeFormatter TIMESTAMP_DATE_TIME_FORMAT_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /*
    Match 1
    Full match	0-536	2020-03-24 11:31:36.458 +8.0 [BigoStatSDK] ReportEvent -> [BLEventDailyReport] time=1585020696,appke...
    Group 1.	0-28	2020-03-24 11:31:36.458 +8.0
    Group 2.	30-41	BigoStatSDK
    Group 3.	43-536	ReportEvent -> [BLEventDailyReport] time=1585020696,appkey=60,ver=4.31.0000(8153),from=AppStore,guid...
     */
    final private Pattern mLogPattern = Pattern.compile("(.+?) \\[(.+?)\\] (.+)");

    @Override
    public LogInfo parseLog(String strText) {
        LogInfo logInfo = new LogInfo();
        try {
            Matcher matcher = mLogPattern.matcher(strText);
            if (matcher.find()) {
                String date = matcher.group(1);
                String tag = matcher.group(2);
                String msg = matcher.group(3);

                logInfo.setTime(date);
                logInfo.setTimestamp(getTimestampFromTime(logInfo.getTime().trim()));
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

    private Pattern mDatePattern = Pattern.compile("(\\.(\\d))$");
    /*
    处理不规范的日期格式
     */
    private long getTimestampFromTime(String time) throws ParseException {
        if (time.contains("+") || time.contains("-")) {
            int indexOfPlus = time.indexOf(" +");
                if (indexOfPlus > -1) {
                    time = time.replace(" +", " +0");
                } else {
                    time = time.replace(" -", " -0");
                }
            Matcher matcher = mDatePattern.matcher(time);
            if (matcher.find()) {
                int deltaTime = Integer.valueOf(matcher.group(2));
                time = matcher.replaceFirst("00");
                if (deltaTime > 0) {
                    return TIMESTAMP_FORMAT_WITH_TIMEZONE.parse(time).getTime() - (long) ((deltaTime / 10.0f) * 3600 * 1000);
                }
            }
        }
        return TIMESTAMP_FORMAT_WITH_TIMEZONE.parse(time).getTime();
    }

    @Override
    public int[] getSupportedColumns() {
        return new int[]{
                LogFilterTableModel.COLUMN_LINE,
                LogFilterTableModel.COLUMN_BOOKMARK,
                LogFilterTableModel.COLUMN_FILE,
                LogFilterTableModel.COLUMN_TIME,
                LogFilterTableModel.COLUMN_DATE,
                LogFilterTableModel.COLUMN_TAG,
                LogFilterTableModel.COLUMN_MESSAGE,
        };
    }
}
