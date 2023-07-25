package com.ocean.smdownloader.InstagramMediaHandler;

public interface InstaMediaRecyclerviewActionListener {
    void onQualityClick(int qualityPosition, InstaMediaDetailData data);
    void onQualitySelected(InstaDownloadTaskData multiDownloadData);
    void onQualityDeselect(InstaDownloadTaskData multiDownloadData);
}
