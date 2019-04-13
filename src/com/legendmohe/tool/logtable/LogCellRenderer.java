package com.legendmohe.tool.logtable;

import com.legendmohe.tool.ILogRenderResolver;
import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.Utils;
import com.legendmohe.tool.config.Constant;
import com.legendmohe.tool.logflow.LogFlowManager;
import com.legendmohe.tool.logtable.model.LogFilterTableModel;

import java.awt.Color;
import java.awt.Component;
import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
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

    private final Border HIGH_LIGHT_NORMAL_BORDER_BOTTOM;
    private final Border HIGH_LIGHT_ERROR_BORDER_BOTTOM;
    private final int HIGH_LIGHT_BORDER_WIDTH = 2;

    private final int BORDER_WIDTH = 1;
    private final Color BORDER_COLOR = new Color(100, 100, 100);

    boolean mIsDataChanged;
    private JTable mTable;
    private ILogRenderResolver mResolver;
    private int mColumnIdx;

    public LogCellRenderer(int iIndex, JTable table, ILogRenderResolver resolver) {
        super();
        mColumnIdx = iIndex;
        HIGH_LIGHT_NORMAL_BORDER_BOTTOM = BorderFactory.createCompoundBorder(null, BorderFactory.createMatteBorder(0, 0, HIGH_LIGHT_BORDER_WIDTH, 0, new Color(Constant.COLOR_LOG_FLOW_NORMAL)));
        HIGH_LIGHT_ERROR_BORDER_BOTTOM = BorderFactory.createCompoundBorder(null, BorderFactory.createMatteBorder(0, 0, HIGH_LIGHT_BORDER_WIDTH, 0, new Color(Constant.COLOR_LOG_FLOW_ERROR)));

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
        this.mTable.getSelectionModel().addListSelectionListener(mListSelectionListener);
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
        renderLogFlow(isSelected, logInfo, column, c);
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
                            .append(Utils.escapeHTML(strText.substring(lastEnd, start)))
                            .append("<span style=\"background-color:#").append(color).append("\"><b>")
                            .append(Utils.escapeHTML(strText.substring(start, end)))
                            .append("</b></span>");
                } else {
                    tmpReplaceHolder
                            .append(Utils.escapeHTML(strText.substring(lastEnd, start)))
                            .append("<font color=#").append(color).append("><b>")
                            .append(Utils.escapeHTML(strText.substring(start, end)))
                            .append("</b></font>");
                }
                lastEnd = end;
                result = matcher.find();
            } while (result);

            // 结尾部分
            tmpReplaceHolder.append(Utils.escapeHTML(strText.substring(lastEnd)));
            strText = tmpReplaceHolder.toString();
            mIsDataChanged = true;
        }
        return strText;
    }

    ///////////////////////////////////log flow///////////////////////////////////

    private void renderLogFlow(boolean isSelected, LogInfo logInfo, int column, Component c) {
        // log flow显示逻辑
        JLabel label = (JLabel) c;
        if (mResolver.isShowLogFlowResult()) {
            List<LogFlowManager.FlowResultLine> flowResults = logInfo.getFlowResults();
            if (flowResults != null && flowResults.size() > 0) {
                if (mResolver.getMinShownColumn() == column) {
                    if (logInfo.hasErrorFlowResult()) {
                        label.setIcon(Utils.createImageIcon(new Color(Constant.COLOR_LOG_FLOW_ERROR), 14, 14));
                    } else {
                        label.setIcon(Utils.createImageIcon(new Color(Constant.COLOR_LOG_FLOW_NORMAL), 14, 14));
                    }
                }
                // 需要高亮的
                if (mFlowHighLightLines.contains(logInfo.getLine())) {
                    if (logInfo.hasErrorFlowResult()) {
                        label.setBackground(new Color(Constant.COLOR_LOG_FLOW_ERROR_LINE));
                    } else {
                        label.setBackground(new Color(Constant.COLOR_LOG_FLOW_NORMAL_LINE));
                    }
                }
                return;
            }
        }
        label.setIcon(null);
    }

    private Set<Integer> mFlowHighLightLines = new HashSet<>();

    private List<Integer> mSelectedRowsCache = new ArrayList<>();

    private final ListSelectionListener mListSelectionListener = new ListSelectionListener() {
        @Override
        public void valueChanged(ListSelectionEvent e) {
            List<Integer> selectedRows = Utils.toIntegerList(mTable.getSelectedRows());
            if (mSelectedRowsCache.equals(selectedRows)) {
                return;
            }
            int oldFlowSize = mFlowHighLightLines.size();
            mFlowHighLightLines.clear();
            mSelectedRowsCache = new ArrayList<>(selectedRows);
            if (selectedRows.size() > 0) {
                for (int selectedRow : selectedRows) {
                    LogInfo logInfo = ((LogFilterTableModel) mTable.getModel()).getRow(selectedRow);
                    List<LogFlowManager.FlowResultLine> flowResults = logInfo.getFlowResults();
                    if (flowResults != null && flowResults.size() > 0) {
                        for (LogFlowManager.FlowResultLine flowResult : flowResults) {
                            for (LogFlowManager.FlowResultLine resultLine : flowResult.flowResult.resultLines) {
                                mFlowHighLightLines.add(resultLine.logInfo.getLine());
                            }
                        }
                    }
                }
                if (mFlowHighLightLines.size() > 0 || oldFlowSize != 0) {
                    EventQueue.invokeLater(() -> mTable.repaint());
                }
            }
        }
    };
}
