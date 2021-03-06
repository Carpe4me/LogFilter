package com.legendmohe.tool;

import com.legendmohe.tool.logflow.LogFlowManager;
import com.legendmohe.tool.logtable.model.LogFilterTableModel;
import com.legendmohe.tool.util.T;
import com.legendmohe.tool.util.Utils;

import java.awt.Color;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class LogInfo {
    public static final int LOG_LV_VERBOSE = 1;
    public static final int LOG_LV_DEBUG = LOG_LV_VERBOSE << 1;
    public static final int LOG_LV_INFO = LOG_LV_DEBUG << 1;
    public static final int LOG_LV_WARN = LOG_LV_INFO << 1;
    public static final int LOG_LV_ERROR = LOG_LV_WARN << 1;
    public static final int LOG_LV_FATAL = LOG_LV_ERROR << 1;
    public static final int LOG_LV_ALL = LOG_LV_VERBOSE | LOG_LV_DEBUG | LOG_LV_INFO
            | LOG_LV_WARN | LOG_LV_ERROR | LOG_LV_FATAL;

    private boolean m_bMarked;
    private String m_strBookmark = "";
    private String m_strDate = "";
    private Integer m_intLine = -1;
    private String m_strTime = "";
    private String m_strLogLV = "";
    private String m_strPid = "";
    private String m_strThread = "";
    private String m_strTag = "";
    private String m_strMessage = "";
    private String m_strFileName = "";
    private long m_timestamp = -1l;
    private Color m_TextColor;
    private TYPE mType = TYPE.SYSTEM;
    private List<LogFlowManager.FlowResultLine> mFlowResults;
    private boolean isSingleMsgLine = false;

    public void display() {
        T.d("=============================================");
        T.d("m_bMarked      = " + isMarked());
        T.d("m_strBookmark  = " + getBookmark());
        T.d("m_strDate      = " + getDate());
        T.d("m_intLine      = " + getLine());
        T.d("m_strTime      = " + getTime());
        T.d("m_strLogLV     = " + getLogLV());
        T.d("m_strPid       = " + getPid());
        T.d("m_strThread    = " + getThread());
        T.d("m_strTag       = " + getTag());
        T.d("m_strMessage   = " + getMessage());
        T.d("m_timestamp   = " + getTimestamp());
        T.d("=============================================");
    }

    /*
        public static final int COLUMN_LINE = 0;
        public static final int COLUMN_FILE = 1;
        public static final int COLUMN_DATE = 2;
        public static final int COLUMN_TIME = 3;
        public static final int COLUMN_LOGLV = 4;
        public static final int COLUMN_PID = 5;
        public static final int COLUMN_THREAD = 6;
        public static final int COLUMN_TAG = 7;
        public static final int COLUMN_BOOKMARK = 8;
        public static final int COLUMN_MESSAGE = 9;
     */
    public Object getContentByColumn(int col) {
        switch (col) {
            case LogFilterTableModel.COLUMN_LINE:
                return getLine();
            case LogFilterTableModel.COLUMN_FILE:
                return getFileName();
            case LogFilterTableModel.COLUMN_DATE:
                return getDate();
            case LogFilterTableModel.COLUMN_TIME:
                return getTime();
            case LogFilterTableModel.COLUMN_LOGLV:
                return getLogLV();
            case LogFilterTableModel.COLUMN_PID:
                return getPid();
            case LogFilterTableModel.COLUMN_THREAD:
                return getThread();
            case LogFilterTableModel.COLUMN_TAG:
                return getTag();
            case LogFilterTableModel.COLUMN_BOOKMARK:
                return getBookmark();
            case LogFilterTableModel.COLUMN_MESSAGE:
                return getMessage();
        }
        return null;
    }

    public boolean findText(String src) {
        Pattern pattern = Utils.findPatternOrCreate(src);

        if (!Utils.isEmpty(getMessage()) && pattern.matcher(getMessage()).find()) {
            return true;
        }
        if (!Utils.isEmpty(getTag()) && pattern.matcher(getTag()).find()) {
            return true;
        }
        if (!Utils.isEmpty(getBookmark()) && pattern.matcher(getBookmark()).find()) {
            return true;
        }
        if (!Utils.isEmpty(getDate()) && pattern.matcher(getDate()).find()) {
            return true;
        }
        if (!Utils.isEmpty(getTime()) && pattern.matcher(getTime()).find()) {
            return true;
        }
        if (!Utils.isEmpty(getLogLV()) && pattern.matcher(getLogLV()).find()) {
            return true;
        }
        if (!Utils.isEmpty(getPid()) && pattern.matcher(getPid()).find()) {
            return true;
        }
        if (!Utils.isEmpty(getThread()) && pattern.matcher(getThread()).find()) {
            return true;
        }
        if (!Utils.isEmpty(getFileName()) && pattern.matcher(getFileName()).find()) {
            return true;
        }
        return false;
    }

    public boolean isMarked() {
        return m_bMarked;
    }

    public void setMarked(boolean m_bMarked) {
        this.m_bMarked = m_bMarked;
    }

    public String getBookmark() {
        return m_strBookmark;
    }

    public void setBookmark(String m_strBookmark) {
        this.m_strBookmark = m_strBookmark;
    }

    public String getDate() {
        return m_strDate;
    }

    public void setDate(String m_strDate) {
        this.m_strDate = m_strDate;
    }

    public Integer getLine() {
        return m_intLine;
    }

    public void setLine(Integer m_intLine) {
        this.m_intLine = m_intLine;
    }

    public String getTime() {
        return m_strTime;
    }

    public void setTime(String m_strTime) {
        this.m_strTime = m_strTime;
    }

    public String getLogLV() {
        return m_strLogLV;
    }

    public void setLogLV(String m_strLogLV) {
        this.m_strLogLV = m_strLogLV;
    }

    public String getPid() {
        return m_strPid;
    }

    public void setPid(String m_strPid) {
        this.m_strPid = m_strPid;
    }

    public String getThread() {
        return m_strThread;
    }

    public void setThread(String m_strThread) {
        this.m_strThread = m_strThread;
    }

    public String getTag() {
        return m_strTag;
    }

    public void setTag(String m_strTag) {
        this.m_strTag = m_strTag;
    }

    public String getMessage() {
        return m_strMessage;
    }

    public void setMessage(String m_strMessage) {
        this.m_strMessage = m_strMessage;
    }

    public String getFileName() {
        return m_strFileName;
    }

    public void setFileName(String fileName) {
        this.m_strFileName = fileName;
    }

    public long getTimestamp() {
        return m_timestamp;
    }

    public void setTimestamp(long m_timestamp) {
        this.m_timestamp = m_timestamp;
    }

    public Color getTextColor() {
        return m_TextColor;
    }

    public void setTextColor(Color m_TextColor) {
        this.m_TextColor = m_TextColor;
    }

    public List<LogFlowManager.FlowResultLine> getFlowResults() {
        return mFlowResults == null ? null : mFlowResults.stream().filter(flowResultLine -> flowResultLine.isValid).collect(Collectors.toList());
    }

    public void setFlowResults(List<LogFlowManager.FlowResultLine> flowResult) {
        mFlowResults = flowResult;
    }

    public boolean hasErrorFlowResult() {
        for (LogFlowManager.FlowResultLine line : mFlowResults) {
            if (line.isValid && line.flowResult.errorCause != null) {
                return true;
            }
        }
        return false;
    }

    public boolean hasFlowResults() {
        return mFlowResults != null && mFlowResults.size() > 0;
    }

    public boolean isSingleMsgLine() {
        return isSingleMsgLine;
    }

    public void setSingleMsgLine(boolean singleMsgLine) {
        isSingleMsgLine = singleMsgLine;
    }

    public TYPE getType() {
        return mType;
    }

    public void setType(TYPE mType) {
        this.mType = mType;
    }

    public enum TYPE {
        SYSTEM, DUMP_OF_SERVICE
    }
}
