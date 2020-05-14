package com.legendmohe.tool.logtable.model;

import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.util.T;

import java.util.ArrayList;

import javax.swing.table.AbstractTableModel;

public class LogFilterTableModel extends AbstractTableModel {
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
    public static final int COLUMN_MAX = 10;

    private static final long serialVersionUID = 1L;

    public static String ColName[] = {"Line", "File", "Date", "Time", "LogLV", "Pid", "Thread", "Tag", "Bookmark", "Message"};
    public static int ColWidth[] = {50, 50, 50, 100, 20, 50, 50, 100, 100, 600};
    public static int DEFAULT_WIDTH[] = {50, 50, 50, 100, 20, 50, 50, 100, 100, 600};

    ArrayList<LogInfo> m_arData;

    public static void setColumnWidth(int nColumn, int nWidth) {
        T.d("nWidth = " + nWidth);
        if (nWidth >= DEFAULT_WIDTH[nColumn])
            ColWidth[nColumn] = nWidth;
    }

    public int getColumnCount() {
        return ColName.length;
    }

    public synchronized int getRowCount() {
        if (m_arData != null)
            return m_arData.size();
        else
            return 0;
    }

    public String getColumnName(int col) {
        return ColName[col];
    }

    public synchronized Object getValueAt(int rowIndex, int columnIndex) {
        if (rowIndex < m_arData.size()) {
            return m_arData.get(rowIndex).getContentByColumn(columnIndex);
        }
        return null;
    }

    public synchronized LogInfo getRow(int row) {
        return (row >= 0 && row < m_arData.size()) ? m_arData.get(row) : null;
    }

    public synchronized void setData(ArrayList<LogInfo> arData) {
        m_arData = arData;
    }
}
