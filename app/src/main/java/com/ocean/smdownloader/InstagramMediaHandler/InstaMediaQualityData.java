package com.ocean.smdownloader.InstagramMediaHandler;

public class InstaMediaQualityData {
    private final int position;
    private String quality;
    private long size;
    private String link;
    private boolean isSelected;

    public InstaMediaQualityData(int position, String quality, long size, String link) {
        this.position = position;
        this.quality = quality;
        this.size = size;
        this.link = link;
        isSelected = false;
    }

    public int getPosition() {
        return position;
    }

    public String getQuality() {
        return quality;
    }

    public void setQuality(String quality) {
        this.quality = quality;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isSelected() {
        return isSelected;
    }
}
