package com.ocean.smdownloader.YTMediaHandler;

public class YTMediaQualityData {
    private final String type;
    private final String mimeType;
    private final String quality;
    private final String primaryUrl;
    private String secondaryUrl;
    private long primarySize;
    private long secondarySize;


    public YTMediaQualityData(String type, String mimeType, String quality, String primaryUrl, long primarySize) {
        this.type = type;
        this.mimeType = mimeType;
        this.quality = quality;
        this.primaryUrl = primaryUrl;
        this.primarySize = primarySize;
    }

    public String getType() {
        return type;
    }

    public String getMimeType() {
        return mimeType;
    }

    public String getQuality() {
        return quality;
    }

    public String getPrimaryUrl() {
        return primaryUrl;
    }

    public void setSecondaryUrl(String secondaryUrl) {
        this.secondaryUrl = secondaryUrl;
    }

    public String getSecondaryUrl() {
        return secondaryUrl;
    }

    public long getPrimarySize() {
        return primarySize;
    }

    public void setPrimarySize(long primarySize) {
        this.primarySize = primarySize;
    }

    public void setSecondarySize(long secondarySize) {
        this.secondarySize = secondarySize;
    }

    public long getSecondarySize() {
        return secondarySize;
    }
}
