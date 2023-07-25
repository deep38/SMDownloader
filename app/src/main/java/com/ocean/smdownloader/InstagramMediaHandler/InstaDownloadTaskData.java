package com.ocean.smdownloader.InstagramMediaHandler;

public class InstaDownloadTaskData {
    private final InstaMediaDetailData data;
    private final int qualityIndex;

    public InstaDownloadTaskData(InstaMediaDetailData data, int qualityIndex) {
        this.data = data;
        this.qualityIndex = qualityIndex;
    }

    public InstaMediaDetailData getData() {
        return data;
    }

    public int getQualityIndex() {
        return qualityIndex;
    }


}
