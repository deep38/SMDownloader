package com.ocean.smdownloader.Download;

public interface MediaFileHandlerListener {
    void onFileCreated(String primaryFilePath, String secondaryFilePath, boolean isOverwrite);
    void onCanceled();
    void onError(String fileName, String error);
}
