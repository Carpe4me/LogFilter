package com.legendmohe.tool.parser;

import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.util.T;
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
public class IMODevLogParser extends AbstractLogParser {

    private static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("MM-dd HH:mm:ss.SSS");
    private static final DateFormat TIMESTAMP_FORMAT_2 = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    private static final DateTimeFormatter TIMESTAMP_DATE_TIME_FORMAT_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /*
        Match 1
        Full match	0-124	03-24 18:10:43.459 32351 32699 D Nerv-LINK_CONNECT: LinkConnector:trySendData 1 group_id:6 link_id:1...
        Group 1.	0-18	03-24 18:10:43.459
        Group 2.	19-31	32351 32699
        Group 3.	19-24	32351
        Group 4.	25-30	32699
        Group 5.	31-32	D
        Group 6.	33-50	Nerv-LINK_CONNECT
        Group 7.	51-124	 LinkConnector:trySendData 1 group_id:6 link_id:10 resendData:0 leftLen:0
        Match 2
        Full match	126-483	[2020-03-25 09:56:40.852] I Network4: ---> TCP : {"method":"forward_to_server","data":{"ack":1,"ssid...
        Group 1.	127-150	2020-03-25 09:56:40.852
        Group 5.	152-153	I
        Group 6.	154-162	Network4
        Group 7.	163-483	 ---> TCP : {"method":"forward_to_server","data":{"ack":1,"ssid":"2m0Yhp6MvOHL3IpEF","messages":[{"data":{"data":{"uid":"1100001721525563","ssid":"2m0Yhp6MvOHL3IpEF"},"request_id":"RyNuLh1A","method":"get_story_objects"},"seq":14,"to":{"system":"broadcastproxy"},"from":{"system":"client","ssid":"2m0Yhp6MvOHL3IpEF"}}]}}
     */
    final private Pattern mLogPattern = Pattern.compile("\\[?(\\d.+?)\\]?\\s+((\\d+)\\s+(\\d+)\\s+)?(\\w+)\\s+(.+?)\\s*:(.+)");

    @Override
    public LogInfo parseLog(String strText) {
        LogInfo logInfo = new LogInfo();
        try {
            Matcher matcher = mLogPattern.matcher(strText);
            if (matcher.find()) {
                String date = matcher.group(1);
                String pid = matcher.group(3);
                String tid = matcher.group(4);
                String level = matcher.group(5);
                String tag = matcher.group(6);
                String msg = matcher.group(7);

                logInfo.setTime(date);

                Date parseDate;
                if (date.indexOf("-") < 4) {
                    parseDate = TIMESTAMP_FORMAT.parse(logInfo.getTime());
                } else {
                    parseDate = TIMESTAMP_FORMAT_2.parse(logInfo.getTime());
                }
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
            ex.printStackTrace();
            T.e(ex.getMessage());
            logInfo = new LogInfo();
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
                LogFilterTableModel.COLUMN_THREAD,
                LogFilterTableModel.COLUMN_PID,
                LogFilterTableModel.COLUMN_MESSAGE,
        };
    }
}
