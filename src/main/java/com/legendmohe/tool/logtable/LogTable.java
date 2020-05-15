package com.legendmohe.tool.logtable;

import com.legendmohe.tool.util.EventBus;
import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.util.T;
import com.legendmohe.tool.config.ThemeConstant;
import com.legendmohe.tool.diff.DiffService;
import com.legendmohe.tool.logflow.LogFlowManager;
import com.legendmohe.tool.logtable.model.LogFilterTableModel;
import com.legendmohe.tool.view.FixPopup;
import com.legendmohe.tool.view.LogFlowDialog;

import java.awt.EventQueue;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumn;

public class LogTable extends BaseLogTable {
    private static final long serialVersionUID = 1L;
    private DiffService mDiffService;

    public LogTable(LogFilterTableModel tableModel, BaseLogTableListener listener) {
        super(tableModel, listener);
        initListener();
    }

    private void initListener() {
        addMouseListener(new MouseAdapter() {

            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                int row = rowAtPoint(p);
                int column = columnAtPoint(p);
                if (row < 0 || row > getRowCount()) {
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2) {
                        if (column != LogFilterTableModel.COLUMN_BOOKMARK) {
                            LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
                            logInfo.setMarked(!logInfo.isMarked());
                            mBaseLogTableListener.markLogInfo(row, logInfo.getLine() - 1, logInfo.isMarked());
                        }
                    } else if (m_bAltPressed) {
                        LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
                        if (column == LogFilterTableModel.COLUMN_TAG) {
                            appendFilterShowTag((String) logInfo.getContentByColumn(column));
                            mBaseLogTableListener.postEvent(new EventBus.Event(EventBus.TYPE.EVENT_CHANGE_FILTER_SHOW_TAG));
                        } else if (column == LogFilterTableModel.COLUMN_TIME) {
                            mBaseLogTableListener.postEvent(
                                    new EventBus.Event(EventBus.TYPE.EVENT_CHANGE_FILTER_FROM_TIME, logInfo.getTime())
                            );
                        }
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {

                    boolean hasSelected = false;
                    for (int sRow : getSelectedRows()) {
                        if (sRow == row) {
                            hasSelected = true;
                            break;
                        }
                    }
                    if (!hasSelected) {
                        setRowSelectionInterval(row, row);
                        setColumnSelectionInterval(column, column);
                    }

                    T.d("m_bAltPressed = " + m_bAltPressed);
                    LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
                    if (m_bAltPressed) {
                        if (column == LogFilterTableModel.COLUMN_TAG) {
                            appendFilterRemoveTag((String) logInfo.getContentByColumn(column));
                            mBaseLogTableListener.postEvent(new EventBus.Event(EventBus.TYPE.EVENT_CHANGE_FILTER_REMOVE_TAG));
                        } else if (column == LogFilterTableModel.COLUMN_TIME) {
                            mBaseLogTableListener.postEvent(
                                    new EventBus.Event(EventBus.TYPE.EVENT_CHANGE_FILTER_TO_TIME, logInfo.getTime())
                            );
                        }
                    } else {
                        JPopupMenu popup = createRightClickPopUp(logInfo);
                        popup.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }
        });
        getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                ListSelectionModel lsm = (ListSelectionModel) e.getSource();
                mMaxSelectedRow = lsm.getMaxSelectionIndex();
                mMinSelectedRow = lsm.getMinSelectionIndex();
            }
        });
        getTableHeader().addMouseListener(new ColumnHeaderListener());
        getColumnModel().addColumnModelListener(mTableColumnWidthListener);
    }


    private TableColumnModelListener mTableColumnWidthListener = new TableColumnModelListener() {

        @Override
        public void columnMarginChanged(ChangeEvent e) {
            TableColumn tableColumn = getTableHeader().getResizingColumn();
            if (tableColumn != null) {
                int colIdx = tableColumn.getModelIndex();
                int width = tableColumn.getWidth();
                LogFilterTableModel.ColWidth[colIdx] = width;
            }
        }

        @Override
        public void columnAdded(TableColumnModelEvent e) {

        }

        @Override
        public void columnRemoved(TableColumnModelEvent e) {

        }

        @Override
        public void columnMoved(TableColumnModelEvent e) {

        }

        @Override
        public void columnSelectionChanged(ListSelectionEvent e) {

        }
    };

    private JPopupMenu createRightClickPopUp(LogInfo logInfo) {

        JPopupMenu menuPopup = new JPopupMenu();
        JMenuItem copycolumnToClipboard = new JMenuItem(new AbstractAction("copy column to clipboard") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selColumns = getSelectedColumns();
                if (selColumns.length != 0) {
                    copySelectedColumn(selColumns);
                }
            }
        });
        JMenuItem copyRowToClipboard = new JMenuItem(new AbstractAction("copy row to clipboard") {
            @Override
            public void actionPerformed(ActionEvent e) {
                copySelectedRows();
            }
        });
        JMenuItem markMenuItem = new JMenuItem(new AbstractAction("mark/unmark") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = getSelectedRows();
                for (int selectedRow : selectedRows) {
                    LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(selectedRow);
                    logInfo.setMarked(!logInfo.isMarked());
                    mBaseLogTableListener.markLogInfo(selectedRow, logInfo.getLine() - 1, logInfo.isMarked());
                }
            }
        });
        JMenuItem showInSubTable = new JMenuItem(new AbstractAction("show in mark table") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = getSelectedRows();
                for (int selectedRow : selectedRows) {
                    LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(selectedRow);

//                    logInfo.setMarked(!logInfo.isMarked());
                    mBaseLogTableListener.showInMarkTable(selectedRow, logInfo.getLine() - 1);
                }
            }
        });
        JMenuItem showRow = new JMenuItem(new AbstractAction("show row") {
            @Override
            public void actionPerformed(ActionEvent e) {
                mBaseLogTableListener.showRowsContent(getFormatSelectedRows(new int[]{}));
            }
        });
        JMenuItem findInDiffMenuItem = new JMenuItem(new AbstractAction("find in connected LogFilter") {
            @Override
            public void actionPerformed(ActionEvent e) {
                LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(getSelectedRow());
                String target = logInfo.getContentByColumn(getSelectedColumn()).toString();
                if (target.trim().length() != 0) {
                    if (mDiffService != null) {
                        mDiffService.writeDiffCommand(
                                DiffService.DiffServiceCmdType.FIND,
                                target
                        );
                    }
                }
            }
        });

        JMenuItem findSimilarInDiffMenuItem = new JMenuItem(new AbstractAction("find similar in connected LogFilter") {
            @Override
            public void actionPerformed(ActionEvent e) {
                LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(getSelectedRow());
                String target = logInfo.getContentByColumn(getSelectedColumn()).toString();
                if (target.trim().length() != 0) {
                    if (mDiffService != null) {
                        mDiffService.writeDiffCommand(
                                DiffService.DiffServiceCmdType.FIND_SIMILAR,
                                target
                        );
                    }
                }
            }
        });

        JMenuItem findTimestampInDiffMenuItem = new JMenuItem(new AbstractAction("find timestamp in connected LogFilter") {
            @Override
            public void actionPerformed(ActionEvent e) {
                LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(getSelectedRow());
                if (logInfo.getTimestamp() > 0) {
                    if (mDiffService != null) {
                        mDiffService.writeDiffCommand(
                                DiffService.DiffServiceCmdType.FIND_TIMESTAMP,
                                String.valueOf(logInfo.getTimestamp())
                        );
                    }
                }
            }
        });

        JMenuItem compareMenuItem = new JMenuItem(new AbstractAction("compare with selected in connected LogFilter") {
            @Override
            public void actionPerformed(ActionEvent e) {
                String target = getFormatSelectedRows(new int[]{LogFilterTableModel.COLUMN_LINE, LogFilterTableModel.COLUMN_DATE});
                if (target != null && target.length() != 0) {
                    if (mDiffService != null) {
                        mDiffService.writeDiffCommand(
                                DiffService.DiffServiceCmdType.COMPARE,
                                target
                        );
                    }
                }
            }
        });

        JMenuItem logFlowMenuItem = new JMenuItem(new AbstractAction("show logflow in dialog") {
            @Override
            public void actionPerformed(ActionEvent e) {
                LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(getSelectedRow());
                showCurrentFlowResultWithSelection(logInfo);
            }
        });

        menuPopup.add(markMenuItem);
        menuPopup.add(showInSubTable);
        menuPopup.add(showRow);
        menuPopup.add(copycolumnToClipboard);
        menuPopup.add(copyRowToClipboard);
        if (logInfo.hasFlowResults()) {
            menuPopup.add(logFlowMenuItem);
        }
        if (mDiffService != null && mDiffService.isDiffConnected()) {
            if (getSelectedRowCount() == 1) {
                menuPopup.add(findInDiffMenuItem);
                menuPopup.add(findTimestampInDiffMenuItem);
                menuPopup.add(findSimilarInDiffMenuItem);
            } else {
                menuPopup.add(compareMenuItem);
            }
        }

        return menuPopup;
    }

    public boolean isCellEditable(int row, int column) {
        return column == LogFilterTableModel.COLUMN_BOOKMARK;
    }

    public DiffService getDiffService() {
        return mDiffService;
    }

    public void setDiffService(DiffService mDiffService) {
        this.mDiffService = mDiffService;
    }

    ///////////////////////////////////hover///////////////////////////////////

    private static final int FIX_POPUP_MAX_WIDTH = 350;

    private static final int FIX_POPUP_MIN_WIDTH = 100;

    private static final int FIX_POPUP_MAX_HEIGHT = 200;

    private List<FixPopup> mMsgTipsPopups = new ArrayList<>();

    @Override
    protected void onHoverTriggerShow(Object value, int row, int col) {
        hidePopups();
        showPopup(value, row, col);
    }

    @Override
    protected void onHoverTriggerHide() {
        // 让popup的mouse adapter先执行，否则isMouseEntered()无效
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                hidePopups();
            }
        });
    }

    private void hidePopups(boolean ignoreMouseEntered) {
        Iterator<FixPopup> iter = mMsgTipsPopups.iterator();
        while (iter.hasNext()) {
            FixPopup popup = iter.next();
            if (popup != null && !popup.isPinned() && (ignoreMouseEntered || !popup.isMouseEntered())) {
                popup.hidePopup();
                iter.remove();
            }
        }
    }

    private void hidePopups() {
        hidePopups(false);
    }

    private void showPopup(Object value, int row, int col) {
        // 先隐藏上一个
        hidePopups(true);

        Point location = MouseInfo.getPointerInfo().getLocation();
        Pair<String, Boolean> popupContent = getPopupContent(value, row, col);
        String content = popupContent.getKey();
        boolean hasFlowResult = popupContent.getValue();
        FixPopup popup = new FixPopup(content, FIX_POPUP_MAX_WIDTH, FIX_POPUP_MIN_WIDTH, FIX_POPUP_MAX_HEIGHT, row);
        popup.setListener(new FixPopup.Listener() {
            @Override
            public void onGoButtonClick(FixPopup popup) {
                handlePopupGoButtonClicked(popup);
            }
        });
        if (hasFlowResult) {
            JButton button = new JButton("< show in dialog");
            button.setBorder(null);
            button.setBorderPainted(false);
            button.setContentAreaFilled(false);
            button.setOpaque(false);
            button.setForeground(ThemeConstant.getColorFixPopupButtonBg());
            button.addActionListener(new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
                    showCurrentFlowResultWithSelection(logInfo);
                }
            });
            popup.addBottomComponent(button);
        }
        popup.showPopup(this, location.x, location.y);
        mMsgTipsPopups.add(popup);
    }

    private void showCurrentFlowResultWithSelection(LogInfo logInfo) {
        if (logInfo.hasFlowResults()) {
            LogFlowDialog dialog = new LogFlowDialog(mBaseLogTableListener.getLogFlowManager(), logInfo);
            dialog.setListener(new LogFlowDialog.Listener() {
                @Override
                public void onOkButtonClicked(LogFlowDialog dialog) {
                    dialog.hide();
                }

                @Override
                public void onItemSelected(LogFlowDialog dialog, LogFlowDialog.ResultItem result) {
                    // jump to result line
                    changeSelection(result.logInfo);
                }

                @Override
                public void onMarkItem(LogFlowDialog logFlowDialog, LogFlowDialog.ResultItem resultItem) {
                    LogInfo resultLog = resultItem.logInfo;
                    mBaseLogTableListener.markLogInfo(0, resultLog.getLine() - 1, !resultLog.isMarked());
                }
            });
            dialog.show();
        }
    }

    private Pair<String, Boolean> getPopupContent(Object value, int row, int col) {
        StringBuilder content = new StringBuilder(String.valueOf(value).trim());
        boolean hasFlowResult = false;
        // log flow显示
        if (isShowLogFlowResult()) {
            LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
            List<LogFlowManager.FlowResultLine> flowResults = logInfo.getFlowResults();
            if (flowResults != null && flowResults.size() > 0) {
                content.append("\n");
                for (LogFlowManager.FlowResultLine resultLine : flowResults) {
                    if (resultLine.flowResult.errorCause != null) {
                        content.append("\n").append("[error] ").append(resultLine.flowResult.desc).append(" <-").append(resultLine.flowResult.errorCause);
                    } else {
                        content.append("\n").append("[info] ").append(resultLine.linkDesc);
                    }
                }
                hasFlowResult = true;
            }
        }
        return new Pair<>(content.toString(), hasFlowResult);
    }

    private void handlePopupGoButtonClicked(FixPopup popup) {
        Object context = popup.getContext();
        if (context instanceof Integer) {
            Integer row = (Integer) context;
            changeSelection(row, 0, false, false);
        }
    }

    @Override
    protected boolean getEnableGroupTag() {
        return true;
    }

    public static class Pair<K, V> {
        private K key;
        private V value;

        public Pair(K key, V value) {
            this.key = key;
            this.value = value;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return value;
        }
    }
}
