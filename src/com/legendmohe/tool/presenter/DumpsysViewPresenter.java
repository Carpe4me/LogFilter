package com.legendmohe.tool.presenter;

import com.legendmohe.tool.T;
import com.legendmohe.tool.Utils;
import com.legendmohe.tool.model.DumpsysViewTableModel;
import com.legendmohe.tool.view.DumpsysViewDialog;

import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JTable;

/**
 * Created by xinyu.he on 2016/2/5.
 */
public class DumpsysViewPresenter {
    private final DumpsysViewDialog mDumpsysViewDialog;
    private final String mDeviceId;
    private final String mCmd;

    public DumpsysViewPresenter(DumpsysViewDialog dumpsysViewDialog, String deviceID, String cmd) {
        this.mDumpsysViewDialog = dumpsysViewDialog;
        this.mDeviceId = deviceID;
        this.mCmd = cmd;
    }

    public void onStart() {
        refreshData();
    }

    public void onRefreshButtonPressed() {
        refreshData();
    }

    private void refreshData() {
        new Thread() {
            @Override
            public void run() {
                mDumpsysViewDialog.onStartLoadingPackages();

                DumpsysViewTableModel model = (DumpsysViewTableModel) mDumpsysViewDialog.getMainTable().getModel();
                model.getData().clear();
                model.fireTableDataChanged();

                try {
                    T.d("run dumpsys cmd: " + DumpsysViewPresenter.this.mCmd);
                    String[] getDumpsysCmd = getADBValidCmd(DumpsysViewPresenter.this.mCmd);
                    List<String> dumpsysResult = processCmd(getDumpsysCmd);
                    if (dumpsysResult != null) {
                        for (String item : dumpsysResult) {
                            DumpsysViewTableModel.DumpsysInfo newInfo = new DumpsysViewTableModel.DumpsysInfo();
                            newInfo.line = item;
                            model.getData().add(newInfo);
                        }
                    }
                    model.fireTableDataChanged();
                } catch (Exception e) {
                    T.d(e);
                }

                mDumpsysViewDialog.onStopLoadingPackages();
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

    private List<String> processCmd(String[] cmd) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(cmd);
            processBuilder.redirectErrorStream(true);
            Process oProcess = processBuilder.start();

            BufferedReader stdOut = new BufferedReader(new InputStreamReader(
                    oProcess.getInputStream()));

            String s;
            ArrayList<String> sb = new ArrayList<>();
            while ((s = stdOut.readLine()) != null) {
                if (s.trim().length() != 0)
                    sb.add(s);
            }
            return sb;
        } catch (Exception e) {
            T.e("e = " + e);
            return null;
        }
    }

    public void saveDumpsysInfoToFile(File file) {
        JTable mainTable = mDumpsysViewDialog.getMainTable();
        try {
            FileWriter fos = new FileWriter(file);
            for (int nIndex = 0; nIndex < mainTable.getRowCount(); nIndex++) {
                String info = (String) mainTable.getValueAt(nIndex, 0);
                fos.write(info);
                fos.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onPreSearchResultButtonPressed() {
        gotoPreSearchResult();
    }

    public void onNextSearchResultButtonPressed() {
        gotoNextSearchResult();
    }

    void gotoNextSearchResult() {
        if (mDumpsysViewDialog.getSearchTarget() == null || mDumpsysViewDialog.getSearchTarget().length() == 0) {
            return;
        }

        JTable mainTable = mDumpsysViewDialog.getMainTable();
        int nSeletectRow = mainTable.getSelectedRow();
        Rectangle parent = mainTable.getVisibleRect();
        String searchTarget = mDumpsysViewDialog.getSearchTarget().toLowerCase();

        for (int nIndex = nSeletectRow + 1; nIndex < mainTable.getRowCount(); nIndex++) {
            String info = (String) mainTable.getValueAt(nIndex, 0);
            if (info.toLowerCase().contains(searchTarget)) {
                mDumpsysViewDialog.changeTableSelection(nIndex, 0, false, false, true);
                return;
            }
        }

        for (int nIndex = 0; nIndex < nSeletectRow; nIndex++) {
            String info = (String) mainTable.getValueAt(nIndex, 0);
            if (info.toLowerCase().contains(searchTarget)) {
                mDumpsysViewDialog.changeTableSelection(nIndex, 0, false, false, true);
                return;
            }
        }
    }

    void gotoPreSearchResult() {
        if (mDumpsysViewDialog.getSearchTarget() == null || mDumpsysViewDialog.getSearchTarget().length() == 0) {
            return;
        }

        JTable mainTable = mDumpsysViewDialog.getMainTable();
        int nSeletectRow = mainTable.getSelectedRow();
        Rectangle parent = mainTable.getVisibleRect();
        String searchTarget = mDumpsysViewDialog.getSearchTarget().toLowerCase();

        for (int nIndex = nSeletectRow - 1; nIndex >= 0; nIndex--) {
            String info = (String) mainTable.getValueAt(nIndex, 0);
            if (info.toLowerCase().contains(searchTarget)) {
                mDumpsysViewDialog.changeTableSelection(nIndex, 0, false, false, true);
                return;
            }
        }

        for (int nIndex = mainTable.getRowCount() - 1; nIndex > nSeletectRow; nIndex--) {
            String info = (String) mainTable.getValueAt(nIndex, 0);
            if (info.toLowerCase().contains(searchTarget)) {
                mDumpsysViewDialog.changeTableSelection(nIndex, 0, false, false, true);
                return;
            }
        }
    }
}
