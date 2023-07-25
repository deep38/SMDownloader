package com.ocean.smdownloader.Download;

public interface DownloadTaskListener {
    void onStart(DownloadTaskData taskData);
    void onProgress(DownloadTaskData taskData, int progress);
    void onPause(DownloadTaskData taskData, boolean isSecondaryTask);
    void onResume(DownloadTaskData taskData);
    void onZeroTaskRemain(boolean removeNotification);
    void onComplete(DownloadTaskData taskData, boolean isSecondaryTask);
    void onError(DownloadTaskData taskData, String error, boolean isSecondaryTask);
    void restart(DownloadTaskData taskData);
}
