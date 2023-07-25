package com.ocean.smdownloader.Download;

public interface MediaMuxerTaskListener {
    void onMuxStart(DownloadTaskData taskData);
    void onMuxProgress(DownloadTaskData taskData, int progress);
    void onMuxFailed(DownloadTaskData taskData, String error);
    void onMuxComplete(DownloadTaskData taskData);
}
