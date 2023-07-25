package com.ocean.smdownloader.Internet;

import com.ocean.smdownloader.Download.DownloadTaskData;

public interface ContentLoadListener {
    void onComplete(String result, DownloadTaskData taskData);
    void onFailed(String error,DownloadTaskData taskData);
}
