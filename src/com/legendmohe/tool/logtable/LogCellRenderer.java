package com.legendmohe.tool.logtable;

import com.legendmohe.tool.ILogRenderResolver;
import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.Utils;
import com.legendmohe.tool.config.Constant;
import com.legendmohe.tool.logflow.LogFlowManager;
import com.legendmohe.tool.logtable.model.LogFilterTableModel;

import java.awt.Color;
import java.awt.Component;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    private final int BORDER_WIDTH = 1;
    private final Color BORDER_COLOR = new Color(100, 100, 100);

    boolean mIsDataChanged;
    private JTable mTable;
    private ILogRenderResolver mResolver;

    public LogCellRenderer(JTable table, ILogRenderResolver resolver) {
        super();
        SELECTED_BORDER_TOP = BorderFactory.createCompoundBorder(null, BorderFactory.createMatteBorder(BORDER_WIDTH, 0, 0, 0, BORDER_COLOR));
        SELECTED_BORDER_BOTTOM = BorderFactory.createCompoundBorder(null, BorderFactory.createMatteBorder(0, 0, BORDER_WIDTH, 0, BORDER_COLOR));
        SELECTED_BORDER_LEFT = BorderFactory.createCompoundBorder(null, BorderFactory.createMatteBorder(0, BORDER_WIDTH, 0, 0, BORDER_COLOR));
        SELECTED_BORDER_RIGHT = BorderFactory.createCompoundBorder(null, BorderFactory.createMatteBorder(0, 0, 0, BORDER_WIDTH, BORDER_COLOR));

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
            value = buildCellContent(column, String.valueOf(logInfo.getContentByColumn(column)));
        }
        Component c = super.getTableCellRendererComponent(table,
                value,
                isSelected,
                hasFocus,
                row,
                column);

        renderFont(c);
        renderBackground(isSelected, logInfo, row, column, c);
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
                // 刷新上下的cell（有时候系统不会回调上下的cell的render方法）
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

    private void renderBackground(boolean isSelected, LogInfo logInfo, int row, int column, Component c) {
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

        // log flow显示逻辑
        if (mResolver.getMinShownColumn() == column && mResolver.isShowLogFlowResult()) {
            if (logInfo.getFlowResults() != null && logInfo.getFlowResults().size() > 0) {
                for (LogFlowManager.FlowResultLine flowResult : logInfo.getFlowResults()) {
                    // 如果有大于一个错，就高亮出来
                    if (flowResult.flowResult.errorCause != null) {
                        c.setBackground(new Color(Constant.COLOR_LOG_FLOW_ERROR));
                        break;
                    }
                    c.setBackground(new Color(Constant.COLOR_LOG_FLOW_NORMAL));
                }
                c.setForeground(new Color(Constant.COLOR_LOG_FLOW_TEXT));
            }
        }
    }

    private void renderFont(Component c) {
        c.setFont(getFont().deriveFont(mResolver.getFontSize()));
    }

    private String buildCellContent(int columnIndex, String strText) {
        if (Utils.isEmpty(strText)) {
            return strText;
        }
        mIsDataChanged = false;

        // html里面的空格会被压缩成一个
        strText = strText.replaceAll(" ", "\u00A0");
        // render highlight
        String strHighLight = mResolver.GetHighlight();
        if (!Utils.isEmpty(strHighLight)) {
            strHighLight = strHighLight.replaceAll(" ", "\u00A0");
            strText = highLightCell(strText, strHighLight, "00FF00", true);
        }
        // render filter
        if (columnIndex == LogFilterTableModel.COLUMN_MESSAGE || columnIndex == LogFilterTableModel.COLUMN_TAG) {
            String strFind = columnIndex == LogFilterTableModel.COLUMN_MESSAGE ? mResolver.GetFilterFind() : mResolver.GetFilterShowTag();
            if (!Utils.isEmpty(strFind)) {
                strFind = strFind.replaceAll(" ", "\u00A0");
                strText = highLightCell(strText, strFind, "FF0000", false);
            }
        }
        // render search
        String strSearch = mResolver.GetSearchHighlight();
        if (!Utils.isEmpty(strSearch)) {
            strSearch = strSearch.replaceAll(" ", "\u00A0");
            strText = highLightCell(strText, strSearch, "FFFF00", true);
        }
        if (mIsDataChanged)
            strText = "<html><nobr>" + strText + "</nobr></html>";

        return strText.replace("\t", "    ");
    }

    private static Map<String, Pattern> sPatternCache = new HashMap<>();

    /*
    高亮背景
     */
    private String highLightCell(String strText, String strFind, String color, boolean bUseSpan) {
        if (strFind == null || strFind.length() <= 0 || strText == null || strText.length() <= 0)
            return strText;
        // pattern cache
        Pattern pattern = sPatternCache.computeIfAbsent(strFind, s -> Pattern.compile("(" + s + ")", Pattern.CASE_INSENSITIVE));
        Matcher matcher = pattern.matcher(strText);

        StringBuilder tmpReplaceHolder = new StringBuilder();
        if (matcher.find()) {
            boolean result;
            int lastEnd = 0;
            do {
                int start = matcher.start();
                int end = matcher.end();

                if (bUseSpan) {
                    tmpReplaceHolder
                            .append(strText, lastEnd, start)
                            .append("<span style=\"background-color:#").append(color).append("\"><b>")
                            .append(strText, start, end)
                            .append("</b></span>");
                } else {
                    tmpReplaceHolder
                            .append(strText, lastEnd, start)
                            .append("<font color=#").append(color).append("><b>")
                            .append(strText, start, end)
                            .append("</b></font>");
                }
                lastEnd = end;
                result = matcher.find();
            } while (result);

            // 结尾部分
            tmpReplaceHolder.append(strText, lastEnd, strText.length());
            strText = tmpReplaceHolder.toString();
            mIsDataChanged = true;
        }
        return strText;
    }
}
