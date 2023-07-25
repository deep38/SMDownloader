package com.ocean.smdownloader.Download;

import android.os.AsyncTask;

public class ThreadData {
    private final DownloadThread thread;
    private long downloaded;
    private int progress;

    public ThreadData(DownloadThread thread, long downloaded, int progress) {
        this.thread = thread;
        this.downloaded = downloaded;
        this.progress = progress;
    }

    public DownloadThread getThread() {
        return thread;
    }

    public void setDownloaded(long downloaded) {
        this.downloaded = downloaded;
    }

    public long getDownloaded() {
        return downloaded;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public int getProgress() {
        return progress;
    }
}
