package com.ocean.smdownloader.InstagramMediaHandler;

import android.graphics.Bitmap;

import java.util.ArrayList;

public class InstaMediaDetailData {
    private final int position;
    private String name;
    private String type;
    private Bitmap thumbnail;
    private String thumbnailLink;
    private ArrayList<InstaMediaQualityData> qualities;

    public InstaMediaDetailData(int position, String name, String type, String thumbnailLink, ArrayList<InstaMediaQualityData> qualities) {
        this.position = position;
        this.name = name;
        this.type = type;
        this.thumbnailLink = thumbnailLink;
        this.qualities = qualities;
    }

    public int getPosition() {
        return position;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getType() {
        return type;
    }

    public void setThumbnail(Bitmap thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Bitmap getThumbnail() {
        return thumbnail;
    }

    public String getThumbnailLink() {
        return thumbnailLink;
    }

    public void setThumbnailLink(String thumbnailLink) {
        this.thumbnailLink = thumbnailLink;
    }

    public ArrayList<InstaMediaQualityData> getQualities() {
        return qualities;
    }

    public void setQualities(ArrayList<InstaMediaQualityData> qualities) {
        this.qualities = qualities;
    }
}
