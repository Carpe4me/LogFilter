package com.legendmohe.tool;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/*
***************************************************************************
**          WiseStone Co. Ltd. CONFIDENTIAL AND PROPRIETARY
**        This source is the sole property of WiseStone Co. Ltd.
**      Reproduction or utilization of this source in whole or in part 
**    is forbidden without the written consent of WiseStone Co. Ltd.
***************************************************************************
**                 Copyright (c) 2007 WiseStone Co. Ltd.
**                           All Rights Reserved
***************************************************************************
** Revision History:
** Author                 Date          Version      Description of Changes
** ------------------------------------------------------------------------
** dhwoo     2010. 3. 12.        1.0              Created
*/

public class T {
    //	private final static String PREFIX = "LogFilter";
    private final static String POSTFIX = "[LogFilter]";
    private static Boolean misEnabled = true;

    private static StringBuilder sSavedLogs = new StringBuilder(100);

    public static void enable(Boolean isEnable) {
        misEnabled = isEnable;
    }

    public static void e() {
        if (misEnabled) {
            Exception e = new Exception();
            StackTraceElement callerElement = e.getStackTrace()[1];
            String log = getCurrentTime() +
                    POSTFIX + "[" +
                    callerElement.getFileName() + ":" +
                    callerElement.getMethodName() + ":" +
                    callerElement.getLineNumber() + "]";
            System.out.println(log);
            sSavedLogs.append(log).append("\n");
        }
    }

    public static void e(Object strMsg) {
        if (misEnabled) {
            Exception e = new Exception();
            StackTraceElement callerElement = e.getStackTrace()[1];
            String log = getCurrentTime() +
                    POSTFIX + "[" +
                    callerElement.getFileName() + ":" +
                    callerElement.getMethodName() + ":" +
                    callerElement.getLineNumber() + "]" +
                    strMsg;
            System.out.println(log);
            sSavedLogs.append(log).append("\n");
        }
    }

    public static void w() {
        if (misEnabled) {
            Exception e = new Exception();
            StackTraceElement callerElement = e.getStackTrace()[1];
            String log = getCurrentTime() +
                    POSTFIX + "[" +
                    callerElement.getFileName() + ":" +
                    callerElement.getMethodName() + ":" +
                    callerElement.getLineNumber() + "]";
            System.out.println(log);
            sSavedLogs.append(log).append("\n");
        }
    }

    public static void w(Object strMsg) {
        if (misEnabled) {
            Exception e = new Exception();
            StackTraceElement callerElement = e.getStackTrace()[1];
            String log = getCurrentTime() +
                    POSTFIX + "[" +
                    callerElement.getFileName() + ":" +
                    callerElement.getMethodName() + ":" +
                    callerElement.getLineNumber() + "]" +
                    strMsg;
            System.out.println(log);
            sSavedLogs.append(log).append("\n");
        }
    }

    public static void i() {
        if (misEnabled) {
            Exception e = new Exception();
            StackTraceElement callerElement = e.getStackTrace()[1];
            String log = getCurrentTime() +
                    POSTFIX + "[" +
                    callerElement.getFileName() + ":" +
                    callerElement.getMethodName() + ":" +
                    callerElement.getLineNumber() + "]";
            System.out.println(log);
            sSavedLogs.append(log).append("\n");
        }
    }

    public static void i(Object strMsg) {
        if (misEnabled) {
            Exception e = new Exception();
            StackTraceElement callerElement = e.getStackTrace()[1];
            String log = getCurrentTime() +
                    POSTFIX + "[" +
                    callerElement.getFileName() + ":" +
                    callerElement.getMethodName() + ":" +
                    callerElement.getLineNumber() + "]" +
                    strMsg;
            System.out.println(log);
            sSavedLogs.append(log).append("\n");
        }
    }

    public static void d() {
        if (misEnabled) {
            Exception e = new Exception();
            StackTraceElement callerElement = e.getStackTrace()[1];
            String log = getCurrentTime() +
                    POSTFIX + "[" +
                    callerElement.getFileName() + ":" +
                    callerElement.getMethodName() + ":" +
                    callerElement.getLineNumber() + "]";
            System.out.println(log);
            sSavedLogs.append(log).append("\n");
        }
    }

    public static void d(Object strMsg) {
        if (misEnabled) {
            Exception e = new Exception();
            StackTraceElement callerElement = e.getStackTrace()[1];
            String log = getCurrentTime() +
                    POSTFIX + "[" +
                    callerElement.getFileName() + ":" +
                    callerElement.getMethodName() + ":" +
                    callerElement.getLineNumber() + "]" +
                    strMsg;
            System.out.println(log);
            sSavedLogs.append(log).append("\n");
        }
    }

    public static String getLogBuffer() {
        return sSavedLogs.toString();
    }

    public static String getCurrentTime() {
        long time = System.currentTimeMillis();
        SimpleDateFormat dayTime = new SimpleDateFormat("yyyy-mm-dd hh:mm:ss.SSS");
        return dayTime.format(new Date(time));

    }
}
