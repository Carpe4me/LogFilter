package com.legendmohe.tool.parser;

import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.util.T;
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
public class BigoXLogParser extends AbstractLogParser {

    private static final DateFormat TIMESTAMP_FORMAT_WITH_TIMEZONE = new SimpleDateFormat("yyyy-MM-dd Z HH:mm:ss.SSS");
    private static final DateTimeFormatter TIMESTAMP_DATE_TIME_FORMAT_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    /*
    Match 1
    Full match	0-195	`[I][2018-11-07 +8.0 17:56:26.364][1][RoomProXLog]RoomSessionhandleLoginRoomSessionSuccess (3) roomProperty ==> isTextForbid:false, isOwnerInRoom:true, isOwnerAbsent:false, isPcMicLink:false, isUs`
    Group 1.	1-2	`I`
    Group 2.	4-32	`2018-11-07 +8.0 17:56:26.364`
    Group 3.	34-35	`1`
    Group 4.	37-48	`RoomProXLog`
    Group 5.	49-195	`RoomSessionhandleLoginRoomSessionSuccess (3) roomProperty ==> isTextForbid:false, isOwnerInRoom:true, isOwnerAbsent:false, isPcMicLink:false, isUs`
     */
    final private Pattern mLogPattern = Pattern.compile("\\[(\\w+)\\]\\[(.+?)\\]\\[(\\d+?)\\]\\[(.+?\\]*)\\](.+)");

    @Override
    public LogInfo parseLog(String strText) {
        LogInfo logInfo = new LogInfo();
        try {
            Matcher matcher = mLogPattern.matcher(strText);
            if (matcher.find()) {
                String level = matcher.group(1);
                String date = matcher.group(2);
                String thread = matcher.group(3);
                String tag = matcher.group(4);
                String msg = matcher.group(5);

                logInfo.setTime(date);
                logInfo.setTimestamp(getTimestampFromTime(logInfo.getTime()));
                logInfo.setLogLV(level);
                logInfo.setThread(thread);
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

    private Pattern mDatePattern = Pattern.compile("(\\.(\\d))");
    /*
    处理不规范的日期格式
     */
    private long getTimestampFromTime(String time) throws ParseException {
        if (time.contains("+") || time.contains("-")) {
            int indexOfPlus = time.indexOf(" +");
            int headIndex = indexOfPlus > -1 ? indexOfPlus : time.indexOf(" -");
            int tailIndex = time.indexOf(".");
            if (tailIndex - headIndex == 3) {
                if (indexOfPlus > -1) {
                    time = time.replace(" +", " +0");
                } else {
                    time = time.replace(" -", " -0");
                }
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
                LogFilterTableModel.COLUMN_LOGLV,
                LogFilterTableModel.COLUMN_TAG,
                LogFilterTableModel.COLUMN_THREAD,
                LogFilterTableModel.COLUMN_MESSAGE,
        };
    }
}
