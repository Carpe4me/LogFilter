package com.legendmohe.tool.view;

import com.legendmohe.tool.LogInfo;
import com.legendmohe.tool.T;
import com.legendmohe.tool.logflow.LogFlowManager;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
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

import javafx.util.Pair;

public class LogFlowDialog {
    private List<LogFlowManager.FlowResult> mFlowResults;
    private JLabel label;
    private JOptionPane optionPane;
    private JButton okButton;
    private Listener mListener;
    private JDialog dialog;

    public LogFlowDialog(List<LogFlowManager.FlowResult> flowResultList) {
        mFlowResults = flowResultList;
        label = new JLabel("double click to jump");
        createAndDisplayOptionPane();
    }

    private void createAndDisplayOptionPane() {
        setupButtons();
        JPanel pane = setupLogTable();
        optionPane = new JOptionPane(pane);
        optionPane.setPreferredSize(new Dimension(800, 500));
        optionPane.setOptions(new Object[]{okButton});
        dialog = optionPane.createDialog("Log Flow");
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

    private JPanel setupLogTable() {
        List<ResultItem> dataList = new ArrayList<>();
        for (LogFlowManager.FlowResult flowResult : mFlowResults) {
            for (Pair<LogInfo, LogFlowManager.FlowPatternItem> itemPair : flowResult.infoPair) {
                dataList.add(new ResultItem(
                        itemPair.getKey(),
                        itemPair.getValue(),
                        flowResult.isCompleted
                ));
            }
        }
        // setup result list
        int finalCount = dataList.size();
        AbstractTableModel tableModel = new AbstractTableModel() {

            private String[] columnNames = new String[]{"flow", "time", "log"};

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
                    renderCellContent((JLabel) component, column, result);
                }
                return component;
            }

            private void renderCellContent(JLabel contentLabel, int column, ResultItem result) {
                if (column == 0) {
                    contentLabel.setText(result.patternItem.patternHolder.name);
                } else if (column == 1) {
                    contentLabel.setText(result.logInfo.getTime());
                } else if (column == 2) {
                    contentLabel.setText(result.logInfo.getMessage());
                }
                contentLabel.setToolTipText(result.patternItem.patternHolder.desc);
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

        resultTable.setCellSelectionEnabled(true);
        ListSelectionModel select = resultTable.getSelectionModel();
        select.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        resultTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                int row = resultTable.rowAtPoint(p);
                int column = resultTable.columnAtPoint(p);
                if (row < 0 || row > resultTable.getRowCount()) {
                    return;
                }

                if (e.getClickCount() == 2) {
                    ResultItem result = dataList.get(row);
                    if (mListener != null) {
                        mListener.onItemSelected(LogFlowDialog.this, result);
                    }
                }
            }
        });

        resultTable.getTableHeader().addMouseListener(new ColumnHeaderListener());
        resultTable.setRowHeight(20);
        resultTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

        JPanel panel = new JPanel(new BorderLayout(5, 5));
        panel.add(label, BorderLayout.NORTH);
        panel.add(new JScrollPane(resultTable), BorderLayout.CENTER);
        return panel;
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
        public LogFlowManager.FlowPatternItem patternItem;
        public boolean isCompleted;

        ResultItem(LogInfo logInfo, LogFlowManager.FlowPatternItem patternItem, boolean isCompleted) {
            this.logInfo = logInfo;
            this.patternItem = patternItem;
            this.isCompleted = isCompleted;
        }
    }

    public interface Listener {
        void onOkButtonClicked(LogFlowDialog dialog);

        void onItemSelected(LogFlowDialog dialog, ResultItem result);
    }
}
