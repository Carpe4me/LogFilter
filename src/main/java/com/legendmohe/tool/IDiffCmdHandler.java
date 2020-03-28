package com.legendmohe.tool;

/**
 * Created by hexinyu on 2018/11/8.
 */
public interface IDiffCmdHandler {
    void searchKeyword(String keyword);

    void refreshDiffMenuBar();

    void refreshUIWithDiffState();

    void searchSimilar(String cmd);

    void searchTimestamp(String cmd);

    void compareWithSelectedRows(String targetRows);

    void enableSyncScroll(boolean enable);

    void handleScrollVSyncEvent(String cmd);

    void handleSelectedForwardSyncEvent(String cmd);

    void handleSelectedBackwardSyncEvent(String cmd);
}
