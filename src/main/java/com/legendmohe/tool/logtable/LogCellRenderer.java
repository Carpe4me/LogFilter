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
import java.awt.Font;
import java.util.ArrayList;
import java.util.Collections;
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
import javax.swing.SwingConstants;
import javax.swing.border.Border;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.plaf.BorderUIResource;
import javax.swing.table.DefaultTableCellRenderer;

/**
 * Created by xinyu.he on 2016/1/22.
 */

public class LogCellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 1L;

    public static final int DIM_CONTENT_APLHA = 0x1e;

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
    private final Border SELECTED_BORDER_PADDING;

    private final int BORDER_WIDTH = 1;
    private final Color BORDER_COLOR = Constant.COLOR_LOG_CELL_BORDER;

    private JTable mTable;
    private ILogRenderResolver mResolver;
    private boolean mEnableGroupTag;

    public LogCellRenderer(JTable table, ILogRenderResolver resolver, boolean enableGroupTag) {
        super();
        this.mEnableGroupTag = enableGroupTag;

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
        SELECTED_BORDER_NONE = BorderFactory.createEmptyBorder(0, 0, 0, 0);

        SELECTED_BORDER_PADDING = BorderFactory.createEmptyBorder(0, Constant.LOG_TABLE_CELL_CONTENT_PADDING_LEFT, 0, Constant.LOG_TABLE_CELL_CONTENT_PADDING_RIGHT);

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
        LogFilterTableModel tableModel = (LogFilterTableModel) mTable.getModel();
        LogInfo logInfo;
        // merge tag group
        boolean dimLine = false;
        logInfo = tableModel.getRow(row);
        Object targetContent = logInfo.getContentByColumn(column);
        if (mEnableGroupTag && row >= 0 && !logInfo.isSingleMsgLine() &&
                (column == LogFilterTableModel.COLUMN_TAG || column == LogFilterTableModel.COLUMN_TIME || column == LogFilterTableModel.COLUMN_DATE)) {
            LogInfo lastLogInfo = tableModel.getRow(row - 1);
            if (lastLogInfo == null || (!logInfo.isSingleMsgLine() && lastLogInfo.isSingleMsgLine()) || !lastLogInfo.getContentByColumn(column).equals(targetContent)) {
                dimLine = false;
            } else {
                dimLine = true;
            }
            targetContent = addSuffixOrPrefixToInfoText(logInfo, row, column, (String) targetContent, dimLine);
            value = buildCellContent(column, String.valueOf(targetContent), dimLine);
        } else {
            // 分行log单独处理
            if (logInfo.isSingleMsgLine() && !(column == LogFilterTableModel.COLUMN_MESSAGE || column == LogFilterTableModel.COLUMN_LINE)) {
                value = "";
            } else {
                value = buildCellContent(column, String.valueOf(targetContent), dimLine);
            }
        }
        Component c = super.getTableCellRendererComponent(table,
                value,
                isSelected,
                hasFocus,
                row,
                column);

        if (c != null) {
            renderFont(logInfo, row, column, c);
            renderBackground(isSelected, logInfo, c, dimLine);
            renderBorder(row, column, c);
            renderLogFlow(isSelected, logInfo, column, c);
        }
        return c;
    }

    // 添加前缀，后缀
    // 目前用于为tag column添加后缀
    private String addSuffixOrPrefixToInfoText(LogInfo logInfo, int row, int column, String value, boolean dimLine) {
        if (column == LogFilterTableModel.COLUMN_TAG) {
            if (dimLine) {
                return value + "  ";
            } else {
                return value + " :";
            }
        }
        return value;
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
            }

            Border originalBorder = cc.getBorder();
            cc.setBorder(new BorderUIResource.CompoundBorderUIResource(originalBorder, SELECTED_BORDER_PADDING));
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

    private void renderBackground(boolean isSelected, LogInfo logInfo, Component c, boolean dim) {
        if (c == null || logInfo == null) {
            return;
        }
        if (logInfo.getTextColor() != null) {
            if (dim) {
                c.setForeground(new Color(
                        logInfo.getTextColor().getRed(),
                        logInfo.getTextColor().getGreen(),
                        logInfo.getTextColor().getBlue(),
                        DIM_CONTENT_APLHA
                ));
            } else {
                c.setForeground(logInfo.getTextColor());
            }
        } else {
            c.setForeground(Constant.COLOR_UNKNOWN);
        }
        if (isSelected) {
            if (logInfo.isMarked()) {
                c.setBackground(Constant.COLOR_BOOKMARK2);
            }
        } else if (logInfo.isMarked()) {
            c.setBackground(Constant.COLOR_BOOKMARK);
        } else {
            c.setBackground(Color.WHITE);
        }
    }

    private void renderFont(LogInfo logInfo, int row, int column, Component c) {
        if (column == LogFilterTableModel.COLUMN_LINE) {
            c.setFont(getFont().deriveFont(Font.BOLD, mResolver.getFontSize()));
        } else {
            c.setFont(getFont().deriveFont(Font.PLAIN, mResolver.getFontSize()));
        }
        if (column == LogFilterTableModel.COLUMN_TAG) {
            ((JLabel) c).setHorizontalAlignment(SwingConstants.RIGHT);
        }
    }

    ///////////////////////////////////high light///////////////////////////////////

    /*
    主要逻辑为：
    1. 收集匹配区间
    2. 根据优先级对逐个字符进行染色
    3. 根据染色结果，分区间进行处理
     */
    private String buildCellContent(int columnIndex, String strText, boolean dimLine) {
        if (Utils.isEmpty(strText)) {
            return strText;
        }
        // dim的行不能有html
        if (dimLine) {
            return strText;
        }
        // html里面的空格会被压缩成一个
        strText = strText.replaceAll(" ", "\u00A0");

        // 1. 收集匹配区间
        List<List<HighLightItem>> resultList = new ArrayList<>();
        int resultCount = 0;
        // render filter
        if (columnIndex == LogFilterTableModel.COLUMN_MESSAGE || columnIndex == LogFilterTableModel.COLUMN_TAG) {
            String strFind = columnIndex == LogFilterTableModel.COLUMN_MESSAGE ? mResolver.GetFilterFind() : mResolver.GetFilterShowTag();
            if (!Utils.isEmpty(strFind)) {
                strFind = strFind.replaceAll(" ", "\u00A0");
                List<HighLightItem> highLightItems = collectHighLightResultItems(HIGH_LIGHT_TYPE_FILTER, strText, strFind);
                // collect result
                resultList.add(highLightItems);
                resultCount += highLightItems.size();
            }
        }
        // render highlight
        String strHighLight = mResolver.GetHighlight();
        if (!Utils.isEmpty(strHighLight)) {
            strHighLight = strHighLight.replaceAll(" ", "\u00A0");
            List<HighLightItem> highLightItems = collectHighLightResultItems(HIGH_LIGHT_TYPE_HIGH_LIGHT, strText, strHighLight);
            // collect result
            resultList.add(highLightItems);
            resultCount += highLightItems.size();
        }
        // render search
        String strSearch = mResolver.GetSearchHighlight();
        if (!Utils.isEmpty(strSearch)) {
            strSearch = strSearch.replaceAll(" ", "\u00A0");
            // collect result
            List<HighLightItem> highLightItems = collectHighLightResultItems(HIGH_LIGHT_TYPE_SEARCH, strText, strSearch);
            resultList.add(highLightItems);
            resultCount += highLightItems.size();
        }
        if (resultCount > 0) {
            strText = transformHighItemToString(strText, resultList);
            strText = "<html><nobr >" + strText + "</nobr></html>";
        }

        return strText.replace("\t", "    ");
    }

    private String transformHighItemToString(String src, List<List<HighLightItem>> resultList) {
        if (resultList.size() <= 0) {
            return src;
        }
        // 2. 根据优先级对逐个字符进行染色
        int[] flags = new int[src.length()];
        for (List<HighLightItem> highLightItems : resultList) {
            for (HighLightItem highLightItem : highLightItems) {
                for (int i = highLightItem.start; i < highLightItem.end; i++) {
                    flags[i] = highLightItem.type;
                }
            }
        }
        // 3. 根据染色结果，分区间进行处理
        StringBuilder sb = new StringBuilder();
        int lastType = 0;
        int lastStart = 0;
        int lastEnd = 0;
        boolean inHighLight = false;
        for (int i = 0; i < flags.length; i++) {
            int curType = flags[i];
            if (lastType != curType) {
                if (inHighLight) {
                    HighLightConfig config = sHighConfig.get(lastType);
                    appendHighLight(sb, lastEnd, src, lastStart, i, config.color, config.useSpan);
                    lastEnd = i;
                    inHighLight = false;
                }
                if (curType != 0) {
                    lastStart = i;
                    inHighLight = true;
                }
            }
            lastType = curType;
        }
        // 一直高亮到结尾
        if (inHighLight) {
            HighLightConfig config = sHighConfig.get(lastType);
            appendHighLight(sb, lastEnd, src, lastStart, flags.length, config.color, config.useSpan);
        } else { // 否则，把还没高亮的补上
            sb.append(Utils.escapeHTML(src.substring(lastEnd)));
        }
        return sb.toString();
    }

    private int appendHighLight(StringBuilder rb, int lastEnd, String src, int start, int end, String color, boolean useSpan) {
        if (useSpan) {
            rb
                    .append(Utils.escapeHTML(src.substring(lastEnd, start)))
                    .append("<span style=\"background-color:#").append(color).append("\"><b>")
                    .append(Utils.escapeHTML(src.substring(start, end)))
                    .append("</b></span>");
        } else {
            rb
                    .append(Utils.escapeHTML(src.substring(lastEnd, start)))
                    .append("<font color=#").append(color).append("><b>")
                    .append(Utils.escapeHTML(src.substring(start, end)))
                    .append("</b></font>");
        }
        return end;
    }

    private static Map<String, Pattern> sPatternCache = new HashMap<>();

    /*
     * 收集高亮结果
     */
    private List<HighLightItem> collectHighLightResultItems(int type, String strText, String strFind) {
        if (strFind == null || strFind.length() <= 0 || strText == null || strText.length() <= 0)
            return Collections.emptyList();
        // pattern cache
        Pattern pattern = sPatternCache.computeIfAbsent(strFind, s -> Pattern.compile("(" + s + ")", Pattern.CASE_INSENSITIVE));
        Matcher matcher = pattern.matcher(strText);

        List<HighLightItem> resultItems = new ArrayList<>();
        if (matcher.find()) {
            boolean result;
            do {
                int start = matcher.start();
                int end = matcher.end();
                resultItems.add(new HighLightItem(type, start, end));
                result = matcher.find();
            } while (result);
        }
        return resultItems;
    }

    // 条目的高亮匹配结果
    private static class HighLightItem {
        int type;
        int start;
        int end;

        private HighLightItem(int type, int start, int end) {
            this.type = type;
            this.start = start;
            this.end = end;
        }
    }

    private static final int HIGH_LIGHT_TYPE_FILTER = 1;
    private static final int HIGH_LIGHT_TYPE_HIGH_LIGHT = 2;
    private static final int HIGH_LIGHT_TYPE_SEARCH = 3;

    // 不同高亮类型的配置
    private static Map<Integer, HighLightConfig> sHighConfig = new HashMap<>();

    static {
        sHighConfig.put(HIGH_LIGHT_TYPE_FILTER, new HighLightConfig(HIGH_LIGHT_TYPE_FILTER, Constant.COLOR_HIGH_LIGHT_TYPE_FILTER, false));
        sHighConfig.put(HIGH_LIGHT_TYPE_HIGH_LIGHT, new HighLightConfig(HIGH_LIGHT_TYPE_HIGH_LIGHT, Constant.COLOR_HIGH_LIGHT_TYPE_HIGH_LIGHT, true));
        sHighConfig.put(HIGH_LIGHT_TYPE_SEARCH, new HighLightConfig(HIGH_LIGHT_TYPE_SEARCH, Constant.COLOR_HIGH_LIGHT_TYPE_SEARCH, true));
    }

    // 高亮配置
    private static class HighLightConfig {
        int type;
        String color;
        boolean useSpan;

        private HighLightConfig(int type, String color, boolean useSpan) {
            this.type = type;
            this.color = color;
            this.useSpan = useSpan;
        }
    }
    ///////////////////////////////////log flow///////////////////////////////////

    private void renderLogFlow(boolean isSelected, LogInfo logInfo, int column, Component c) {
        if (logInfo == null) {
            return;
        }
        // log flow显示逻辑
        JLabel label = (JLabel) c;
        if (mResolver.isShowLogFlowResult()) {
            List<LogFlowManager.FlowResultLine> flowResults = logInfo.getFlowResults();
            if (flowResults != null && flowResults.size() > 0) {
                if (mResolver.getMinShownColumn() == column) {
                    if (logInfo.hasErrorFlowResult()) {
                        label.setIcon(Utils.createImageIcon(Constant.COLOR_LOG_FLOW_ERROR, 14, 14));
                    } else {
                        label.setIcon(Utils.createImageIcon(Constant.COLOR_LOG_FLOW_NORMAL, 14, 14));
                    }
                }
                // 需要高亮的
                if (mFlowHighLightLines.contains(logInfo.getLine())) {
                    if (logInfo.hasErrorFlowResult()) {
                        label.setBackground(Constant.COLOR_LOG_FLOW_ERROR_LINE);
                    } else {
                        label.setBackground(Constant.COLOR_LOG_FLOW_NORMAL_LINE);
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
