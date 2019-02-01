package com.legendmohe.tool.parser;

import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.T;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hexinyu on 2018/11/7.
 */
public class BigoXLogParser extends AbstractLogParser {

    private static final DateFormat TIMESTAMP_FORMAT_WITH_TIMEZONE = new SimpleDateFormat("yyyy-MM-dd Z HH:mm:ss.SSS");

    /*
    Match 1
    Full match	0-195	`[I][2018-11-07 +8.0 17:56:26.364][1][RoomProXLog]RoomSessionhandleLoginRoomSessionSuccess (3) roomProperty ==> isTextForbid:false, isOwnerInRoom:true, isOwnerAbsent:false, isPcMicLink:false, isUs`
    Group 1.	1-2	`I`
    Group 2.	4-32	`2018-11-07 +8.0 17:56:26.364`
    Group 3.	34-35	`1`
    Group 4.	37-48	`RoomProXLog`
    Group 5.	49-195	`RoomSessionhandleLoginRoomSessionSuccess (3) roomProperty ==> isTextForbid:false, isOwnerInRoom:true, isOwnerAbsent:false, isPcMicLink:false, isUs`
     */
    final private Pattern mLogPattern = Pattern.compile("\\[(\\w+)\\]\\[(.+?)\\]\\[(\\d+?)\\]\\[(.+?)\\](.+)");

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

                logInfo.setDate(date);
                logInfo.setTime(date);
                logInfo.setTimestamp(getTimestampFromTime(logInfo.getTime()));
                logInfo.setLogLV(level);
                logInfo.setThread(thread);
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

    /*
    处理不规范的日期格式
     */
    private long getTimestampFromTime(String time) throws ParseException {
        if (time.contains(".0")) {
            int indexOfPlus = time.indexOf(" +");
            int headIndex = indexOfPlus > -1 ? indexOfPlus : time.indexOf(" -");
            int tailIndex = time.indexOf(".0");
            if (tailIndex - headIndex == 3) {
                if (indexOfPlus > -1) {
                    time = time.replace(" +", " +0");
                } else {
                    time = time.replace(" -", " -0");
                }
            }
            time = time.replaceFirst("\\.0", "00");
        }
        return TIMESTAMP_FORMAT_WITH_TIMEZONE.parse(time).getTime();
    }

    @Override
    public void loadProcessorFromConfig() {
        // ignore
    }
}
