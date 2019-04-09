package com.legendmohe.tool.logtable;

import com.legendmohe.tool.ILogRenderResolver;
import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.config.Constant;
import com.legendmohe.tool.logtable.model.LogFilterTableModel;

import java.awt.Color;
import java.awt.Component;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Created by xinyu.he on 2016/1/22.
 */

public class LogCellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;

    private final Border SELECTED_BORDER_TOP_LEFT;
    private final Border SELECTED_BORDER_TOP_RIGHT;
    private final Border SELECTED_BORDER_TOP;
    private final Border SELECTED_BORDER_BOTTOM_LEFT;
    private final Border SELECTED_BORDER_BOTTOM_RIGHT;
    private final Border SELECTED_BORDER_BOTTOM;
    private final Border SELECTED_BORDER_LEFT;
    private final Border SELECTED_BORDER_RIGHT;
    private final Border SELECTED_BORDER_TOTAL;
    private final Border SELECTED_BORDER_LEFT_RIGHT;
    private final Border SELECTED_BORDER_LEFT_RIGHT_TOP;
    private final Border SELECTED_BORDER_LEFT_RIGHT_BOTTOM;
    private final Border SELECTED_BORDER_TOP_BOTTOM;
    private final Border SELECTED_BORDER_TOP_BOTTOM_LEFT;
    private final Border SELECTED_BORDER_TOP_BOTTOM_RIGHT;
    private final Border SELECTED_BORDER_NONE;

    private final int BRORDER_WIDTG = 1;
    private final Color BORDER_COLOR = new Color(100, 100, 100);

    boolean mIsDataChanged;
    private JTable mTable;
    private ILogRenderResolver mResolver;

    public LogCellRenderer(JTable table, ILogRenderResolver resolver) {
        super();
        SELECTED_BORDER_TOP = BorderFactory.createCompoundBorder(null, BorderFactory.createMatteBorder(BRORDER_WIDTG, 0, 0, 0, BORDER_COLOR));
        SELECTED_BORDER_BOTTOM = BorderFactory.createCompoundBorder(null, BorderFactory.createMatteBorder(0, 0, BRORDER_WIDTG, 0, BORDER_COLOR));
        SELECTED_BORDER_LEFT = BorderFactory.createCompoundBorder(null, BorderFactory.createMatteBorder(0, BRORDER_WIDTG, 0, 0, BORDER_COLOR));
        SELECTED_BORDER_RIGHT = BorderFactory.createCompoundBorder(null, BorderFactory.createMatteBorder(0, 0, 0, BRORDER_WIDTG, BORDER_COLOR));

        SELECTED_BORDER_TOP_LEFT = BorderFactory.createCompoundBorder(SELECTED_BORDER_TOP, SELECTED_BORDER_LEFT);
        SELECTED_BORDER_TOP_RIGHT = BorderFactory.createCompoundBorder(SELECTED_BORDER_TOP, SELECTED_BORDER_RIGHT);
        SELECTED_BORDER_BOTTOM_LEFT = BorderFactory.createCompoundBorder(SELECTED_BORDER_BOTTOM, SELECTED_BORDER_LEFT);
        SELECTED_BORDER_BOTTOM_RIGHT = BorderFactory.createCompoundBorder(SELECTED_BORDER_BOTTOM, SELECTED_BORDER_RIGHT);

        SELECTED_BORDER_LEFT_RIGHT = BorderFactory.createCompoundBorder(SELECTED_BORDER_LEFT, SELECTED_BORDER_RIGHT);
        SELECTED_BORDER_LEFT_RIGHT_TOP = BorderFactory.createCompoundBorder(SELECTED_BORDER_LEFT_RIGHT, SELECTED_BORDER_TOP);
        SELECTED_BORDER_LEFT_RIGHT_BOTTOM = BorderFactory.createCompoundBorder(SELECTED_BORDER_LEFT_RIGHT, SELECTED_BORDER_BOTTOM);

        SELECTED_BORDER_TOP_BOTTOM = BorderFactory.createCompoundBorder(SELECTED_BORDER_TOP, SELECTED_BORDER_BOTTOM);
        SELECTED_BORDER_TOP_BOTTOM_LEFT = BorderFactory.createCompoundBorder(SELECTED_BORDER_TOP_BOTTOM, SELECTED_BORDER_LEFT);
        SELECTED_BORDER_TOP_BOTTOM_RIGHT = BorderFactory.createCompoundBorder(SELECTED_BORDER_TOP_BOTTOM, SELECTED_BORDER_RIGHT);

        SELECTED_BORDER_TOTAL = BorderFactory.createCompoundBorder(SELECTED_BORDER_TOP_LEFT, SELECTED_BORDER_BOTTOM_RIGHT);
        SELECTED_BORDER_NONE = null;

        this.mTable = table;
        this.mResolver = resolver;
    }

    @Override
    public Component getTableCellRendererComponent(JTable table,
                                                   Object value,
                                                   boolean isSelected,
                                                   boolean hasFocus,
                                                   int row,
                                                   int column) {
        if (!mResolver.isColumnShown(column)) {
            return null;
        }
        LogInfo logInfo = ((LogFilterTableModel) mTable.getModel()).getRow(row);
        if (value != null) {
            value = renderCellContent(column, value.toString());
        }
        Component c = super.getTableCellRendererComponent(table,
                value,
                isSelected,
                hasFocus,
                row,
                column);

        renderFont(c);
        renderBackground(isSelected, logInfo, c);
        renderBorder(row, column, c);
        return c;
    }

    private void renderBorder(int row, int column, Component c) {
        if (c instanceof JComponent) {
            JComponent cc = (JComponent) c;
            if (!mTable.isRowSelected(row - 1) && mTable.isRowSelected(row)) {
                // 如果选中的行是下面连着的
                if (mTable.isRowSelected(row + 1)) {
                    renderBorderFirstLine(column, cc);
                } else {
                    renderBorderSingleLine(column, cc);
                }
            } else if (!mTable.isRowSelected(row + 1) && mTable.isRowSelected(row)) {
                // 如果选中的行是上面连着的
                if (mTable.isRowSelected(row - 1)) {
                    renderBorderLastLine(column, cc);
                } else {
                    renderBorderSingleLine(column, cc);
                }
            } else if (mTable.isRowSelected(row)) {
                renderBorderMiddleLine(column, cc);
            } else {
                renderNoBorderLine(column, cc);
                // 刷新上下的cell（有时候系统不会回调上下的cell的render方DefaultTableModel 法）
                if (mTable.isRowSelected(row - 1)) {
                    ((AbstractTableModel) mTable.getModel()).fireTableCellUpdated(row - 1, column);
                }
                if (mTable.isRowSelected(row + 1)) {
                    ((AbstractTableModel) mTable.getModel()).fireTableCellUpdated(row + 1, column);
                }
            }
        }
    }

    private void renderBorderSingleLine(int column, JComponent cc) {
        if (mResolver.getMinShownColumn() == column) {
            cc.setBorder(SELECTED_BORDER_TOP_BOTTOM_LEFT);
        } else if (mResolver.getMaxShownColumn() == column) {
            cc.setBorder(SELECTED_BORDER_TOP_BOTTOM_RIGHT);
        } else {
            cc.setBorder(SELECTED_BORDER_TOP_BOTTOM);
        }
    }

    private void renderBorderFirstLine(int column, JComponent cc) {
        if (mResolver.getMinShownColumn() == column) {
            cc.setBorder(SELECTED_BORDER_TOP_LEFT);
        } else if (mResolver.getMaxShownColumn() == column) {
            cc.setBorder(SELECTED_BORDER_TOP_RIGHT);
        } else {
            cc.setBorder(SELECTED_BORDER_TOP);
        }
    }

    private void renderBorderLastLine(int column, JComponent cc) {
        if (mResolver.getMinShownColumn() == column) {
            cc.setBorder(SELECTED_BORDER_BOTTOM_LEFT);
        } else if (mResolver.getMaxShownColumn() == column) {
            cc.setBorder(SELECTED_BORDER_BOTTOM_RIGHT);
        } else {
            cc.setBorder(SELECTED_BORDER_BOTTOM);
        }
    }

    private void renderBorderMiddleLine(int column, JComponent cc) {
        if (mResolver.getMinShownColumn() == column) {
            cc.setBorder(SELECTED_BORDER_LEFT);
        } else if (mResolver.getMaxShownColumn() == column) {
            cc.setBorder(SELECTED_BORDER_RIGHT);
        } else {
            cc.setBorder(SELECTED_BORDER_NONE);
        }
    }

    private void renderNoBorderLine(int column, JComponent cc) {
        cc.setBorder(SELECTED_BORDER_NONE);
    }

    private void renderBackground(boolean isSelected, LogInfo logInfo, Component c) {
        c.setForeground(logInfo.getTextColor());
        if (isSelected) {
            c.setFont(getFont().deriveFont(mResolver.getFontSize() + 1));
            if (logInfo.isMarked()) {
                c.setBackground(new Color(Constant.COLOR_BOOKMARK2));
            }
        } else if (logInfo.isMarked()) {
            c.setBackground(new Color(Constant.COLOR_BOOKMARK));
        } else {
            c.setBackground(Color.WHITE);
        }
    }

    private void renderFont(Component c) {
        c.setFont(getFont().deriveFont(mResolver.getFontSize()));
    }

    private String renderCellContent(int columnIndex, String strText) {

        mIsDataChanged = false;

        strText = strText.replace(" ", "\u00A0");
        if (Constant.COLOR_HIGHLIGHT != null && Constant.COLOR_HIGHLIGHT.length > 0) {
            strText = highLightCell(strText, mResolver.GetHighlight(), Constant.COLOR_HIGHLIGHT, true);
        } else {
            strText = highLightCell(strText, mResolver.GetHighlight(), new String[]{"00FF00"}, true);
        }

        if (columnIndex == LogFilterTableModel.COLUMN_MESSAGE || columnIndex == LogFilterTableModel.COLUMN_TAG) {
            String strFind = columnIndex == LogFilterTableModel.COLUMN_MESSAGE ? mResolver.GetFilterFind() : mResolver.GetFilterShowTag();
            strText = highLightCell(strText, strFind, new String[]{"FF0000"}, false);
        }

        strText = highLightCell(strText, mResolver.GetSearchHighlight(), new String[]{"FFFF00"}, true);
        if (mIsDataChanged)
            strText = "<html><nobr>" + strText + "</nobr></html>";

        return strText.replace("\t", "    ");
    }

    private String highLightCell(String strText, String strFind, String[] arColor, boolean bUseSpan) {
        if (strFind == null || strFind.length() <= 0)
            return strText;

        strFind = strFind.replace(" ", "\u00A0");
        StringTokenizer stk = new StringTokenizer(strFind, "|");
        String newText;
        String strToken;
        int nIndex = 0;

        while (stk.hasMoreElements()) {
            if (nIndex >= arColor.length)
                nIndex = 0;
            strToken = stk.nextToken();

            int idx = strText.toLowerCase().indexOf(strToken.toLowerCase());
            if (idx != -1) {
                String prefix = strText.substring(0, idx);
                String suffix = strText.substring(idx + strToken.length());
                String target = strText.substring(idx, idx + strToken.length());

                if (bUseSpan)
                    newText = "<span style=\"background-color:#" + arColor[nIndex] + "\"><b>";
                else
                    newText = "<font color=#" + arColor[nIndex] + "><b>";
                newText += target;
                if (bUseSpan)
                    newText += "</b></span>";
                else
                    newText += "</b></font>";
                strText = prefix + newText + suffix;
                mIsDataChanged = true;
                nIndex++;
            }
        }
        return strText;
    }
}
