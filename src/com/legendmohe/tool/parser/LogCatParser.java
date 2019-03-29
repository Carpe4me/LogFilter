package com.legendmohe.tool.parser;

import com.legendmohe.tool.LogInfo;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.StringTokenizer;

public class LogCatParser extends AbstractLogParser {
    private static final String TOKEN_KERNEL = "<>[]";
    private static final String TOKEN_SPACE = " ";
    private static final String TOKEN_SLASH = "/";
    private static final String TOKEN = "/()";
    private static final String TOKEN_PID = "/() ";
    private static final String TOKEN_MESSAGE = "'";

    public static final DateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("HH:mm:ss.SSS");

    public LogCatParser() {
    }

    @Override
    public LogInfo parseLog(String strText) {
        if (isNormal(strText))
            return getNormal(strText);
        else if (isThreadTime(strText))
            return getThreadTime(strText);
        else if (isKernel(strText))
            return getKernel(strText);
        else {
            LogInfo logInfo = new LogInfo();
            logInfo.setMessage(strText);
            return logInfo;
        }
    }

    ///////////////////////////////////private///////////////////////////////////

    //04-17 09:01:18.910 D/LightsService(  139): BKL : 106
    private boolean isNormal(String strText) {
        if (strText.length() < 22) return false;

        String strLevel = strText.substring(19, 21);
        if (strLevel.equals("D/")
                || strLevel.equals("V/")
                || strLevel.equals("I/")
                || strLevel.equals("W/")
                || strLevel.equals("E/")
                || strLevel.equals("F/")
                )
            return true;
        return false;
    }

    //04-20 12:06:02.125   146   179 D BatteryService: update start
    private boolean isThreadTime(String strText) {
        if (strText.length() < 34) return false;

        String strLevel = strText.substring(31, 33);
        if (strLevel.equals("D ")
                || strLevel.equals("V ")
                || strLevel.equals("I ")
                || strLevel.equals("W ")
                || strLevel.equals("E ")
                || strLevel.equals("F ")
                )
            return true;
        return false;
    }

    //    <4>[19553.494855] [DEBUG] USB_SEL(1) HIGH set USB mode
    private boolean isKernel(String strText) {
        if (strText.length() < 18) return false;

        String strLevel = strText.substring(1, 2);
        if (strLevel.equals("0")
                || strLevel.equals("1")
                || strLevel.equals("2")
                || strLevel.equals("3")
                || strLevel.equals("4")
                || strLevel.equals("5")
                || strLevel.equals("6")
                || strLevel.equals("7")
                )
            return true;
        return false;
    }

    private LogInfo getNormal(String strText) {
        LogInfo logInfo = new LogInfo();

        StringTokenizer stk = new StringTokenizer(strText, TOKEN_PID, false);
        if (stk.hasMoreElements()) {
            logInfo.setDate(stk.nextToken());
        }
        if (stk.hasMoreElements()) {
            logInfo.setTime(stk.nextToken());
            try {
                logInfo.setTimestamp(TIMESTAMP_FORMAT.parse(logInfo.getTime()).getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (stk.hasMoreElements()) {
            logInfo.setLogLV(stk.nextToken().trim());
        }
        if (stk.hasMoreElements()) {
            logInfo.setTag(stk.nextToken().trim());
        }
        if (stk.hasMoreElements()) {
            logInfo.setPid(stk.nextToken().trim());
        }
        if (stk.hasMoreElements()) {
            logInfo.setMessage(stk.nextToken(TOKEN_MESSAGE));
            while (stk.hasMoreElements()) {
                logInfo.setMessage(logInfo.getMessage() + stk.nextToken(TOKEN_MESSAGE));
            }
            logInfo.setMessage(logInfo.getMessage().replaceFirst("\\): ", ""));
        }
        logInfo.setTextColor(getColor(logInfo));
        return logInfo;
    }

    private LogInfo getThreadTime(String strText) {
        LogInfo logInfo = new LogInfo();

        StringTokenizer stk = new StringTokenizer(strText, TOKEN_SPACE, false);
        if (stk.hasMoreElements()) {
            logInfo.setDate(stk.nextToken());
        }
        if (stk.hasMoreElements()) {
            logInfo.setTime(stk.nextToken());
            try {
                logInfo.setTimestamp(TIMESTAMP_FORMAT.parse(logInfo.getTime()).getTime());
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }
        if (stk.hasMoreElements()) {
            logInfo.setPid(stk.nextToken().trim());
        }
        if (stk.hasMoreElements()) {
            logInfo.setThread(stk.nextToken().trim());
        }
        if (stk.hasMoreElements()) {
            logInfo.setLogLV(stk.nextToken().trim());
        }
        if (stk.hasMoreElements()) {
            logInfo.setTag(stk.nextToken().trim());
        }
        if (stk.hasMoreElements()) {
            logInfo.setMessage(stk.nextToken(TOKEN_MESSAGE).trim());
            if (logInfo.getMessage().length() != 0 && logInfo.getMessage().charAt(0) == ':') {
                logInfo.setTag(logInfo.getTag() + ":");
                logInfo.setMessage(logInfo.getMessage().substring(1).trim());
            }
            while (stk.hasMoreElements()) {
                logInfo.setMessage(logInfo.getMessage() + stk.nextToken(TOKEN_MESSAGE));
            }
            logInfo.setMessage(logInfo.getMessage().replaceFirst("\\): ", ""));
        }
        logInfo.setTextColor(getColor(logInfo));
        return logInfo;
    }

    private LogInfo getKernel(String strText) {
        LogInfo logInfo = new LogInfo();

        StringTokenizer stk = new StringTokenizer(strText, TOKEN_KERNEL, false);
        if (stk.hasMoreElements()) {
            logInfo.setLogLV(stk.nextToken());
        }
        if (stk.hasMoreElements()) {
            logInfo.setTime(stk.nextToken());
            try {
                logInfo.setTimestamp(TIMESTAMP_FORMAT.parse(logInfo.getTime()).getTime());
            } catch (ParseException e) {
//                e.printStackTrace();
            }
        }
        if (stk.hasMoreElements()) {
            logInfo.setMessage(stk.nextToken(TOKEN_KERNEL));
            while (stk.hasMoreElements()) {
                logInfo.setMessage(logInfo.getMessage() + " " + stk.nextToken(TOKEN_SPACE));
            }
            logInfo.setMessage(logInfo.getMessage().replaceFirst("  ", ""));
        }
        logInfo.setTextColor(getColor(logInfo));
        return logInfo;
    }
}
