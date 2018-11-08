package com.legendmohe.tool.parser;

import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.T;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by hexinyu on 2018/11/7.
 */
public class BigoXLogParser extends AbstractLogParser {

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

    @Override
    public void loadProcessorFromConfig() {
        // ignore
    }
}
