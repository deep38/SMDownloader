package com.ocean.smdownloader.Download;

public interface RecyclerViewItemActionListener {
    void onPlayPauseButtonClick(DownloadTaskData taskData);
    void onItemClick(DownloadTaskData taskData);
    void onItemSelectionStart();
    void onItemSelectionEnd();
    void onItemSelectionChange(int selected);
}
