package com.legendmohe.tool.logtable;

import com.legendmohe.tool.INotiEvent;
import com.legendmohe.tool.LogFilterMain;
import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.T;

import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.AbstractAction;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingUtilities;

public class SubLogTable extends BaseLogTable {
    private static final long serialVersionUID = 1L;

    public SubLogTable(LogFilterTableModel tablemodel, LogFilterMain filterMain) {
        super(tablemodel, filterMain);
        initListener();
        setHistoryEnable(false);
    }

    private void initListener() {
        addMouseListener(new MouseAdapter() {

            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                int row = rowAtPoint(p);
                int column = columnAtPoint(p);
                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2) {
                        if (column != LogFilterTableModel.COLUMN_BOOKMARK) {
                            LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(row);
                            showInfoInLogTable(logInfo);
                        }
                    } else if (m_bAltPressed) {
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
                    } else {
                        if (e.isPopupTrigger() && e.getComponent() instanceof JTable) {
                            JPopupMenu popup = createRightClickPopUp();
                            popup.show(e.getComponent(), e.getX(), e.getY());
                        }
                    }
                }
            }
        });
        getTableHeader().addMouseListener(new ColumnHeaderListener());
    }

    private void showInfoInLogTable(LogInfo logInfo) {
        mBaseLogTableListener.notiEvent(
                new INotiEvent.EventParam(INotiEvent.TYPE.EVENT_CHANGE_SELECTION, logInfo)
        );
    }

    private JPopupMenu createRightClickPopUp() {

        JPopupMenu menuPopup = new JPopupMenu();

        JMenuItem markMenuItem = new JMenuItem(new AbstractAction("remove") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selectedRows = getSelectedRows();
                LogInfo[] selectedInfo = new LogInfo[selectedRows.length];
                for (int i = 0; i < selectedRows.length; i++) {
                    selectedInfo[i] = ((LogFilterTableModel) getModel()).getRow(selectedRows[i]);
                }
                for (int i = 0; i < selectedRows.length; i++) {
                    LogInfo info = selectedInfo[i];
                    mBaseLogTableListener.markLogInfo(selectedRows[i], info.getLine() - 1, !info.isMarked());
                }

            }
        });
        menuPopup.add(markMenuItem);

        JMenuItem copycolumnToClipboard = new JMenuItem(new AbstractAction("copy column to clipboard") {
            @Override
            public void actionPerformed(ActionEvent e) {
                int[] selColumns = getSelectedColumns();
                if (selColumns.length != 0) {
                    copySelectedColumn(selColumns);
                }
            }
        });
        menuPopup.add(copycolumnToClipboard);
        JMenuItem copyRowToClipboard = new JMenuItem(new AbstractAction("copy row to clipboard") {
            @Override
            public void actionPerformed(ActionEvent e) {
                copySelectedRows();
            }
        });
        menuPopup.add(copyRowToClipboard);

        if (getSelectedRowCount() == 1) {
            JMenuItem showInLogTable = new JMenuItem(new AbstractAction("show in log table") {
                @Override
                public void actionPerformed(ActionEvent e) {
                    LogInfo logInfo = ((LogFilterTableModel) getModel()).getRow(getSelectedRow());
                    showInfoInLogTable(logInfo);
                }
            });
            menuPopup.add(showInLogTable);
        }

        return menuPopup;
    }

    public boolean isCellEditable(int row, int column) {
        return column == LogFilterTableModel.COLUMN_BOOKMARK;
    }
}
