package com.ocean.smdownloader.Download;

public interface DownloadThreadListener {
    void onProgress(DownloadTaskData taskData, int threadNo, int progress, long downloaded, boolean isSecondaryTask);
    void onPause(DownloadTaskData taskData, int threadNo, long downloaded, boolean isSecondaryTask);
    void onComplete(DownloadTaskData taskData, int threadNo, long downloaded, boolean isSecondaryTask);
    void onError(DownloadTaskData taskData, int threadNo, String error, long downloaded, boolean isSecondaryTask);
}
