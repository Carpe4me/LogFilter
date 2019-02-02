package com.legendmohe.tool.logtable;

import com.legendmohe.tool.EventBus;
import com.legendmohe.tool.LogFilterMain;
import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.T;
import com.legendmohe.tool.diff.DiffService;
import com.legendmohe.tool.view.FixPopup;

import java.awt.EventQueue;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
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

    public LogTable(LogFilterTableModel tableModel, LogFilterMain filterMain) {
        super(tableModel, filterMain);
        initListener();
    }

    protected void initListener() {
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
                            if (m_strTagShow.contains("|" + logInfo.getData(column))) {
                                m_strTagShow = m_strTagShow.replace("|" + logInfo.getData(column), "");
                            } else if (m_strTagShow.contains((String) logInfo.getData(column))) {
                                m_strTagShow = m_strTagShow.replace((String) logInfo.getData(column), "");
                            } else {
                                m_strTagShow += "|" + logInfo.getData(column);
                            }
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
                    if (m_bAltPressed) {
                        LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
                        if (column == LogFilterTableModel.COLUMN_TAG) {
                            m_strTagRemove += "|" + logInfo.getData(column);
                            mBaseLogTableListener.postEvent(new EventBus.Event(EventBus.TYPE.EVENT_CHANGE_FILTER_REMOVE_TAG));
                        } else if (column == LogFilterTableModel.COLUMN_TIME) {
                            mBaseLogTableListener.postEvent(
                                    new EventBus.Event(EventBus.TYPE.EVENT_CHANGE_FILTER_TO_TIME, logInfo.getTime())
                            );
                        }
                    } else {
                        if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                            JPopupMenu popup = createRightClickPopUp();
                            popup.show(e.getComponent(), e.getX(), e.getY());
                        }
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

    private JPopupMenu createRightClickPopUp() {

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
                String target = logInfo.getData(getSelectedColumn()).toString();
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
                String target = logInfo.getData(getSelectedColumn()).toString();
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

        menuPopup.add(markMenuItem);
        menuPopup.add(showInSubTable);
        menuPopup.add(showRow);
        menuPopup.add(copycolumnToClipboard);
        menuPopup.add(copyRowToClipboard);
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

    private static final int FIX_POPUP_WIDTH = 350;

    private List<FixPopup> mMsgTipsPopups = new ArrayList<>();

    @Override
    protected void onHoverTrigger(Object value, int row, int col) {
        hidePopups();
        showPopup(value, row, col);
    }

    @Override
    protected void onHoverTimerStop() {
        // 让popup的mouse adapter先执行，否则isMouseEntered()无效
        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                hidePopups();
            }
        });
    }

    private void hidePopups() {
        Iterator<FixPopup> iter = mMsgTipsPopups.iterator();
        while (iter.hasNext()) {
            FixPopup popup = iter.next();
            if (popup != null && !popup.isMouseEntered() && !popup.isPinned()) {
                popup.hidePopup();
                iter.remove();
            }
        }
    }

    private void showPopup(Object value, int row, int col) {
        Point location = MouseInfo.getPointerInfo().getLocation();
        FixPopup popup = new FixPopup(String.valueOf(value), FIX_POPUP_WIDTH, row);
        popup.setListener(new FixPopup.Listener() {
            @Override
            public void onGoButtonClick(FixPopup popup) {
                handlePopupGoButtonClicked(popup);
            }
        });
        popup.showPopup(this, location.x, location.y + 1);
        mMsgTipsPopups.add(popup);
    }

    private void handlePopupGoButtonClicked(FixPopup popup) {
        Object context = popup.getContext();
        if (context instanceof Integer) {
            Integer row = (Integer) context;
            changeSelection(row, 0, false, false);
        }
    }
}
