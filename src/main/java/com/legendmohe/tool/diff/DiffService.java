package com.legendmohe.tool.diff;

import com.legendmohe.tool.IDiffCmdHandler;
import com.legendmohe.tool.LogFilterComponent;
import com.legendmohe.tool.T;
import com.legendmohe.tool.thirdparty.json.JSONObject;

import java.io.IOException;
import java.net.Socket;

/**
 * Created by xinyu.he on 2016/1/12.
 */
public class DiffService {

    private DiffServer mDiffServer;
    private DiffClient mDiffClient;
    private IDiffCmdHandler mCmdHandler;

    private DiffServiceType mDiffServiceType;
    private boolean mIsDiffConnected;

    public DiffService(LogFilterComponent mainPanel, int serverPort) {
        mCmdHandler = mainPanel;
        setupDiffServer(serverPort);
    }

    DiffClient.DiffClientListener mDiffClientListener = new DiffClient.DiffClientListener() {
        @Override
        public String onReceiveString(String input) {
            return handleReceiveDiffCmd(input);
        }

        @Override
        public void onConnected() {
            mDiffServiceType = DiffServiceType.AS_CLIENT;
            mIsDiffConnected = true;

            mCmdHandler.refreshUIWithDiffState();
            mCmdHandler.refreshDiffMenuBar();
        }

        @Override
        public void onDisconnected() {
            mDiffServiceType = null;
            mIsDiffConnected = false;

            mCmdHandler.refreshUIWithDiffState();
            mCmdHandler.refreshDiffMenuBar();
        }
    };


    DiffServer.DiffServerListener mDiffServerListener = new DiffServer.DiffServerListener() {
        @Override
        public String onReceiveString(String input) {
            return handleReceiveDiffCmd(input);
        }

        @Override
        public void onClientConnected(Socket clientSocket) {
            mDiffServiceType = DiffServiceType.AS_SERVER;
            mIsDiffConnected = true;
            mCmdHandler.refreshUIWithDiffState();
        }

        @Override
        public void onClientDisconnected(Socket clientSocket) {
            mDiffServiceType = null;
            mIsDiffConnected = false;
            mCmdHandler.refreshUIWithDiffState();
        }
    };

    public boolean setupDiffClient(String serverPort) {
        try {
            mDiffClient = new DiffClient(Integer.valueOf(serverPort), mDiffClientListener);
            mDiffClient.start();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public void disconnectDiffClient() {
        if (mDiffClient != null) {
            mDiffClient.cleanup();
        }
    }

    private void setupDiffServer(int port) {
        try {
            mDiffServer = new DiffServer(port, mDiffServerListener);
            T.d("bind port:" + port);
        } catch (IOException e) {
            e.printStackTrace();

            mDiffServer.cleanup();
            mDiffServer = null;
        }
        if (mDiffServer != null)
            mDiffServer.start();
    }

    public boolean isDiffConnected() {
        return mIsDiffConnected;
    }

    private String handleReceiveDiffCmd(String input) {
        JSONObject responseJson = new JSONObject(input);
        String type = responseJson.getString("type");
        String cmd = responseJson.getString("cmd");
        T.d("receive type:" + type + " cmd:" + cmd);
        if (type.equals(DiffServiceCmdType.SYNC_SCROLL_V.toString())) {
            mCmdHandler.handleScrollVSyncEvent(cmd);
        } else if (type.equals(DiffServiceCmdType.FIND.toString())) {
            mCmdHandler.searchKeyword(cmd);
        } else if (type.equals(DiffServiceCmdType.FIND_SIMILAR.toString())) {
            mCmdHandler.searchSimilar(cmd);
        } else if (type.equals(DiffServiceCmdType.FIND_TIMESTAMP.toString())) {
            mCmdHandler.searchTimestamp(cmd);
        } else if (type.equals(DiffServiceCmdType.COMPARE.toString())) {
            mCmdHandler.compareWithSelectedRows(cmd);
        } else if (type.equals(DiffServiceCmdType.SYNC_SELECTED_FORWARD.toString())) {
            mCmdHandler.handleSelectedForwardSyncEvent(cmd);
        } else if (type.equals(DiffServiceCmdType.SYNC_SELECTED_BACKWARD.toString())) {
            mCmdHandler.handleSelectedBackwardSyncEvent(cmd);
        }
        return null;
    }

    public void writeDiffCommand(DiffServiceCmdType cmdType, String cmd) {
        if (!mIsDiffConnected) {
            T.d("service disconnected.");
            return;
        }
        JSONObject jsonReq = new JSONObject();
        jsonReq.put("type", cmdType);
        jsonReq.put("cmd", cmd);
        String req = jsonReq.toString();
        T.d("send req to target: " + req);

        switch (getDiffServiceType()) {
            case AS_CLIENT:
                mDiffClient.writeStringToServer(req);
                break;
            case AS_SERVER:
                mDiffServer.writeStringToClient(req);
                break;
            default:
                break;
        }
    }

    public DiffServiceType getDiffServiceType() {
        return mDiffServiceType;
    }

    //////////////////////////////////////////////////////////////////////

    public enum DiffServiceType {
        AS_CLIENT, AS_SERVER
    }

    public enum DiffServiceCmdType {
        FIND,
        FIND_SIMILAR,
        FIND_TIMESTAMP,
        COMPARE,
        SYNC_SELECTED_FORWARD,
        SYNC_SELECTED_BACKWARD,
        SYNC_SCROLL_V
    }
}
