package com.legendmohe.tool.view;

import com.legendmohe.tool.logtable.model.PackageViewTableModel;
import com.legendmohe.tool.util.T;
import com.legendmohe.tool.util.Utils;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Comparator;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableColumnModel;

/**
 * Created by xinyu.he on 2016/2/5.
 */
public class PackageViewDialog extends JDialog {
    private final PackageViewDialogListener mListener;
    private PackageViewPresenter mPackageViewPresenter;
    private PackageViewTableModel mPackageViewTableModel;
    private JTable mMainTable;
    private JButton mRefreshButton;
    private JProgressBar mProgressBar;

    public PackageViewDialog(Frame frame, String title, String deviceId, PackageViewDialogListener listener) {
        super(frame, title, true);

        mPackageViewPresenter = new PackageViewPresenter(this, deviceId);
        this.mListener = listener;

        Container c = getContentPane();
        c.setLayout(new BorderLayout());
        initMainTable(c);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));

        mProgressBar = new JProgressBar();
        mProgressBar.setVisible(false);
        buttonPanel.add(mProgressBar);

        mRefreshButton = new JButton("Refresh");
        mRefreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                mPackageViewPresenter.onRefreshButtonPressed();
            }
        });
        buttonPanel.add(mRefreshButton);
        c.add(buttonPanel, BorderLayout.SOUTH);

        this.pack();

        mPackageViewPresenter.onStart();
    }

    private void initMainTable(Container c) {
        mPackageViewTableModel = new PackageViewTableModel();
        mMainTable = new JTable(mPackageViewTableModel);
        mMainTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        mMainTable.getTableHeader().setReorderingAllowed(false);
        mMainTable.setOpaque(false);
        mMainTable.setAutoscrolls(false);
//        mMainTable.setIntercellSpacing(new Dimension(0, 0));

        mMainTable.getColumnModel().getColumn(0).setResizable(true);
        mMainTable.getColumnModel().getColumn(0).setMinWidth(80);
        mMainTable.getColumnModel().getColumn(0).setPreferredWidth(80);

        mMainTable.getColumnModel().getColumn(1).setResizable(true);
        mMainTable.getColumnModel().getColumn(1).setMinWidth(60);
        mMainTable.getColumnModel().getColumn(1).setPreferredWidth(320);

        mMainTable.getTableHeader().addMouseListener(new ColumnHeaderListener());
        mMainTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                Point p = e.getPoint();
                int row = mMainTable.rowAtPoint(p);
                int column = mMainTable.columnAtPoint(p);
                if (row < 0 || row > mMainTable.getRowCount()) {
                    return;
                }

                if (SwingUtilities.isLeftMouseButton(e)) {
                    if (e.getClickCount() == 2) {
                        String value = (String) mMainTable.getModel().getValueAt(row, 0);
                        if (mListener != null) {
                            mListener.onFliterPidSelected(value);
                        }
                    }
                }
            }
        });


        JScrollPane scrollPane = new JScrollPane(mMainTable);
        c.add(scrollPane, BorderLayout.CENTER);
    }

    public JTable getMainTable() {
        return mMainTable;
    }

    public class ColumnHeaderListener extends MouseAdapter {
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
                mPackageViewPresenter.onColumnHeaderClicked(vColIndex);
            }
        }
    }

    public void onStartLoadingPackages() {
        mProgressBar.setVisible(true);
        mProgressBar.setIndeterminate(true);
        mRefreshButton.setEnabled(false);
    }

    public void onStopLoadingPackages() {
        mProgressBar.setVisible(false);
        mProgressBar.setIndeterminate(false);
        mRefreshButton.setEnabled(true);
    }

    public interface PackageViewDialogListener {

        void onFliterPidSelected(String value);
    }

    ///////////////////////////////////presenter///////////////////////////////////

    private static class PackageViewPresenter {
        private final PackageViewDialog mPackageViewDialog;
        private final String mDeviceId;

        public PackageViewPresenter(PackageViewDialog packageViewDialog, String deviceID) {
            this.mPackageViewDialog = packageViewDialog;
            this.mDeviceId = deviceID;
        }

        public void onStart() {
            refreshPackageData();
        }

        public void onRefreshButtonPressed() {
            refreshPackageData();
        }

        private void refreshPackageData() {
            new Thread() {
                @Override
                public void run() {
                    mPackageViewDialog.onStartLoadingPackages();

                    PackageViewTableModel model = (PackageViewTableModel) mPackageViewDialog.getMainTable().getModel();
                    model.getData().clear();
                    model.fireTableDataChanged();

                    T.d("getting zygoteID");
                    String[] getPPIDCmd = getADBValidCmd("ps | grep zygote");
                    String PPIDResult = processCmd(getPPIDCmd);
                    if (PPIDResult != null) {
                        try {
                            String zygoteID = PPIDResult.split("\\s+")[1];
                            T.d("got zygoteID: " + zygoteID);
                            String[] getPackageCmd = getADBValidCmd("ps | grep " + "\\\" " + zygoteID + " \\\"");
                            String packageResult = processCmd(getPackageCmd);
                            if (packageResult != null) {
                                for (String item : packageResult.split("\n")) {
                                    T.d("got package: " + item);
                                    String[] infos = item.split("\\s+");
                                    PackageViewTableModel.PackageInfo newInfo = new PackageViewTableModel.PackageInfo();
                                    newInfo.pid = infos[1];
                                    newInfo.name = infos[8];
                                    model.getData().add(newInfo);
                                }
                            }
                            model.fireTableDataChanged();
                        } catch (Exception e) {
                            T.d(e);
                        }
                    }

                    mPackageViewDialog.onStopLoadingPackages();
                }
            }.start();
        }

        private String[] getADBValidCmd(String customCmd) {
            if (mDeviceId != null && mDeviceId.length() != 0) {
                customCmd = "adb -s " + mDeviceId + " shell \"" + customCmd + "\"";
            } else {
                customCmd = "adb shell \"" + customCmd + "\"";
            }
            String[] cmd;
            if (Utils.isWindows()) {
                cmd = new String[]{"cmd.exe", "/C", customCmd};
            } else {
                cmd = new String[]{"/bin/bash", "-l", "-c", customCmd};
            }
            return cmd;
        }

        private String processCmd(String[] cmd) {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(cmd);
                processBuilder.redirectErrorStream(true);
                Process oProcess = processBuilder.start();

                BufferedReader stdOut = new BufferedReader(new InputStreamReader(
                        oProcess.getInputStream()));

                String s;
                StringBuffer sb = new StringBuffer();
                while ((s = stdOut.readLine()) != null) {
                    if (s.trim().length() != 0) {
                        sb.append(s.trim());
                        sb.append("\n");
                    }
                }
                return sb.toString();
            } catch (Exception e) {
                T.e("e = " + e);
                return null;
            }
        }

        public void onColumnHeaderClicked(final int vColIndex) {
            PackageViewTableModel model = (PackageViewTableModel) this.mPackageViewDialog.getMainTable().getModel();
            boolean ascend = true;
            if (model.getData().size() > 1) {
                if (model.getData().get(0).getValue(vColIndex).compareTo(
                        model.getData().get(model.getData().size() - 1).getValue(vColIndex)
                ) < 0) {
                    ascend = false;
                }
            }

            final boolean finalAscend = ascend;
            model.getData().sort(new Comparator<PackageViewTableModel.PackageInfo>() {
                @Override
                public int compare(PackageViewTableModel.PackageInfo o1, PackageViewTableModel.PackageInfo o2) {
                    if (finalAscend)
                        return o1.getValue(vColIndex).compareTo(o2.getValue(vColIndex));
                    else
                        return o2.getValue(vColIndex).compareTo(o1.getValue(vColIndex));
                }
            });
            model.fireTableDataChanged();
        }
    }
}