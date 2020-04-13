package com.legendmohe.tool.view;

import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.T;
import com.legendmohe.tool.logflow.LogFlowManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.EventQueue;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JViewport;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;

public class LogFlowDialog {
    private LogFlowManager logFlowManager;
    private Map<String, List<LogFlowManager.FlowResult>> mFlowResults;
    private JOptionPane optionPane;
    private JButton okButton;
    private Listener mListener;
    private JDialog dialog;

    public LogFlowDialog(LogFlowManager logFlowManager, Map<String, List<LogFlowManager.FlowResult>> flowResultList) {
        this.logFlowManager = logFlowManager;
        mFlowResults = flowResultList;
        createAndDisplayOptionPane();
    }

    public LogFlowDialog(LogFlowManager logFlowManager, LogInfo logInfo) {
        this.logFlowManager = logFlowManager;
        // 从某行result line拿到所属result的所有line
        Map<String, List<LogFlowManager.FlowResult>> resultMap = new HashMap<>();
        for (LogFlowManager.FlowResultLine resultLine : logInfo.getFlowResults()) {
            resultMap.putIfAbsent(resultLine.flowResult.name, new ArrayList<>());
            resultMap.get(resultLine.flowResult.name).add(resultLine.flowResult);
        }
        mFlowResults = resultMap;
        createAndDisplayOptionPane();
    }

    private void createAndDisplayOptionPane() {
        setupButtons();

        JTabbedPane tabbedPane = new JTabbedPane();
        List<String> sortedName = new ArrayList<>(mFlowResults.keySet());
        sortedName.sort(new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                // 应该不会有性能问题
                return logFlowManager.getResultIndex(o1) - logFlowManager.getResultIndex(o2);
            }
        });
        for (String resultName : sortedName) {
            List<LogFlowManager.FlowResult> resultList = mFlowResults.get(resultName);
            JComponent table = setupLogTable(resultList);
            tabbedPane.addTab(resultName, table);
        }

        JPanel panel = new JPanel(new BorderLayout(1, 1));
        panel.add(new JLabel("double click to jump"), BorderLayout.NORTH);
        panel.add(tabbedPane, BorderLayout.CENTER);

        optionPane = new JOptionPane(panel);
        optionPane.setPreferredSize(new Dimension(800, 500));
        optionPane.setOptions(new Object[]{okButton});
        dialog = optionPane.createDialog("Log Flow");
        dialog.setModal(false);
    }

    private void setupButtons() {
        okButton = new JButton("Ok");
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (mListener != null) {
                    mListener.onOkButtonClicked(LogFlowDialog.this);
                }
            }
        });
    }

    private JScrollPane setupLogTable(List<LogFlowManager.FlowResult> resultList) {
        List<ResultItem> dataList = new ArrayList<>();
        for (LogFlowManager.FlowResult flowResult : resultList) {
            for (LogFlowManager.FlowResultLine line : flowResult.resultLines) {
                dataList.add(new ResultItem(
                        line.logInfo,
                        flowResult,
                        line,
                        flowResult.errorCause == null
                ));
            }
        }
        // setup result list
        int finalCount = dataList.size();
        AbstractTableModel tableModel = new AbstractTableModel() {

            private String[] columnNames = new String[]{"flow", "desc", "time", "log"};

            @Override
            public String getColumnName(int column) {
                return columnNames[column];
            }

            @Override
            public int getRowCount() {
                return finalCount;
            }

            @Override
            public int getColumnCount() {
                return columnNames.length;
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                return dataList.get(rowIndex);
            }
        };
        JTable resultTable = new JTable(tableModel);
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                ResultItem result = (ResultItem) resultTable.getModel().getValueAt(row, column);
                Component component = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (component instanceof JLabel) {
                    renderCellContent((JLabel) component, row, column, result);
                }
                return component;
            }

            private void renderCellContent(JLabel contentLabel, int row, int column, ResultItem result) {
                if (column == 0) {
                    if (result.resultLine.isStartLine) {
                        contentLabel.setText(result.flowResult.name);
                    } else if (!result.isCompleted) {
                        contentLabel.setText(result.flowResult.errorCause);
                    } else {
                        contentLabel.setText("");
                    }
                    contentLabel.setToolTipText(result.flowResult.desc);
                } else if (column == 1) {
                    contentLabel.setText(result.resultLine.linkDesc);
                } else if (column == 2) {
                    contentLabel.setText(result.logInfo.getTime());
                    contentLabel.setToolTipText(result.logInfo.getTime());
                } else if (column == 3) {
                    contentLabel.setText(result.logInfo.getMessage());
                    contentLabel.setToolTipText(result.resultLine.desc);
                }
                if (!result.isCompleted) {
                    contentLabel.setBackground(Color.RED);
                } else {
                    contentLabel.setBackground(Color.WHITE);
                }
            }
        };
        for (int i = 0; i < resultTable.getColumnCount(); i++) {
            resultTable.getColumnModel().getColumn(i).setCellRenderer(renderer);
        }

        resultTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                int row = resultTable.rowAtPoint(p);
                int column = resultTable.columnAtPoint(p);

                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (row < 0 || row > resultTable.getRowCount()) {
                        return;
                    }

                    if (e.getClickCount() == 2) {
                        ResultItem result = dataList.get(row);
                        if (mListener != null) {
                            mListener.onItemSelected(LogFlowDialog.this, result);
                        }
                    }
                } else if (SwingUtilities.isRightMouseButton(e)) {
                    boolean hasSelected = false;
                    for (int sRow : resultTable.getSelectedRows()) {
                        if (sRow == row) {
                            hasSelected = true;
                            break;
                        }
                    }
                    if (!hasSelected) {
                        resultTable.setRowSelectionInterval(row, row);
                        resultTable.setColumnSelectionInterval(column, column);
                    }

                    ResultItem result = dataList.get(row);
                    JPopupMenu popup = createRightClickPopUp(result);
                    popup.show(e.getComponent(), e.getX(), e.getY());
                }
            }

            private JPopupMenu createRightClickPopUp(ResultItem result) {
                JPopupMenu menuPopup = new JPopupMenu();
                JMenuItem markItem = new JMenuItem(new AbstractAction("mark") {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        if (result == null) {
                            return;
                        }
                        if (mListener != null) {
                            mListener.onMarkItem(LogFlowDialog.this, result);
                        }
                    }
                });
                menuPopup.add(markItem);
                return menuPopup;
            }
        });

        resultTable.setCellSelectionEnabled(true);
        ListSelectionModel select = resultTable.getSelectionModel();
        select.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        resultTable.getTableHeader().addMouseListener(new ColumnHeaderListener());
        resultTable.setRowHeight(20);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        resultTable.getTableHeader().setReorderingAllowed(false);
        resultTable.setOpaque(false);
        resultTable.setAutoscrolls(false);

        // pack all columns
        EventQueue.invokeLater(new Runnable() {
            public void run() {
                for (int i = 0; i < tableModel.getColumnCount(); i++) {
                    packColumn(resultTable, i, 5);
                }
            }
        });

        return new JScrollPane(resultTable);
    }

    public void setListener(Listener listener) {
        mListener = listener;
    }

    public void show() {
        dialog.setVisible(true);
    }

    public void hide() {
        dialog.setVisible(false);
    }

    ///////////////////////////////////private///////////////////////////////////

    public void packColumn(JTable table, int vColIndex, int margin) {
        DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
        TableColumn col = colModel.getColumn(vColIndex);
        int width = 0;

        JViewport viewport = (JViewport) table.getParent();
        Rectangle viewRect = viewport.getViewRect();
        int nFirst = table.rowAtPoint(new Point(0, viewRect.y));
        int nLast = table.rowAtPoint(new Point(0, viewRect.height - 1));

        if (nLast < 0) {
            nLast = table.getRowCount();
        }
        // Get width of column header
        TableCellRenderer renderer;
        Component comp;
        // Get maximum width of column data
        for (int r = nFirst; r < nFirst + nLast; r++) {
            renderer = table.getCellRenderer(r, vColIndex);
            comp = renderer.getTableCellRendererComponent(
                    table, table.getValueAt(r, vColIndex), false, false, r, vColIndex);
            width = Math.max(width, comp.getPreferredSize().width);
        }

        // Add margin
        width += 2 * margin;

        // Set the width
        col.setPreferredWidth(width);

        table.getColumnModel().getColumn(vColIndex).setResizable(true);
        table.getColumnModel().getColumn(vColIndex).setMaxWidth(width * 1000);
        table.getColumnModel().getColumn(vColIndex).setMinWidth(1);
        table.getColumnModel().getColumn(vColIndex).setWidth(width);
        table.getColumnModel().getColumn(vColIndex).setPreferredWidth(width);
    }

    private class ColumnHeaderListener extends MouseAdapter {

        public void mouseClicked(MouseEvent evt) {

            if (SwingUtilities.isLeftMouseButton(evt) && evt.getClickCount() == 2) {
                JTable table = ((JTableHeader) evt.getSource()).getTable();
                TableColumnModel colModel = table.getColumnModel();

                // The index of the column whose header was clicked
                int vColIndex = colModel.getColumnIndexAtX(evt.getX());

                if (vColIndex == -1) {
                    T.d("vColIndex == -1");
                    return;
                }
                packColumn(table, vColIndex, 5);
            }
        }
    }

    //////////////////////////////////////////////////////////////////////

    public static class ResultItem {
        public LogInfo logInfo;
        public LogFlowManager.FlowResult flowResult;
        public LogFlowManager.FlowResultLine resultLine;
        public boolean isCompleted;

        ResultItem(LogInfo logInfo, LogFlowManager.FlowResult flowResult, LogFlowManager.FlowResultLine resultLine, boolean isCompleted) {
            this.logInfo = logInfo;
            this.flowResult = flowResult;
            this.resultLine = resultLine;
            this.isCompleted = isCompleted;
        }
    }

    public interface Listener {
        void onOkButtonClicked(LogFlowDialog dialog);

        void onItemSelected(LogFlowDialog dialog, ResultItem result);

        void onMarkItem(LogFlowDialog logFlowDialog, ResultItem resultItem);
    }
}
