package com.ocean.smdownloader.Download;

import android.os.Parcel;
import android.os.Parcelable;

public class DownloadTaskData {
    private final int id;
    private String name;
    private final String type;
    private final String source;
    private final String thumbnailPath;
    private String primaryLink;
    private String secondaryLink;
    private String primaryFilePath;
    private String secondaryFilePath;
    private long primaryDownloadedSize;
    private long secondaryDownloadedSize;
    private final long primaryFileSize;
    private final long secondaryFileSize;
    private int primaryStatus;
    private int secondaryStatus;
    private boolean isSelected;

    public DownloadTaskData(int id, String name, String type, String source, String thumbnailPath, String primaryLink, String secondaryLink, String primaryFilePath, String secondaryFilePath, long primaryDownloadedSize, long secondaryDownloadedSize, long primaryFileSize, long secondaryFileSize, int primaryStatus, int secondaryStatus) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.source = source;
        this.thumbnailPath = thumbnailPath;
        this.primaryLink = primaryLink;
        this.secondaryLink = secondaryLink;
        this.primaryFilePath = primaryFilePath;
        this.secondaryFilePath = secondaryFilePath;
        this.primaryDownloadedSize = primaryDownloadedSize;
        this.secondaryDownloadedSize = secondaryDownloadedSize;
        this.primaryFileSize = primaryFileSize;
        this.secondaryFileSize = secondaryFileSize;
        this.primaryStatus = primaryStatus;
        this.secondaryStatus = secondaryStatus;
        this.isSelected = false;
    }

    public int getId() {
        return id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getType() {
        return type;
    }

    public String getSource() {
        return source;
    }

    public String getThumbnailPath() {
        return thumbnailPath;
    }

    public void setPrimaryLink(String primaryLink) {
        this.primaryLink = primaryLink;
    }

    public String getPrimaryLink() {
        return primaryLink;
    }

    public void setSecondaryLink(String secondaryLink) {
        this.secondaryLink = secondaryLink;
    }

    public String getSecondaryLink() {
        return secondaryLink;
    }

    public void setPrimaryFilePath(String primaryFilePath) {
        this.primaryFilePath = primaryFilePath;
    }

    public String getPrimaryFilePath() {
        return primaryFilePath;
    }

    public void setSecondaryFilePath(String secondaryFilePath) {
        this.secondaryFilePath = secondaryFilePath;
    }

    public String getSecondaryFilePath() {
        return secondaryFilePath;
    }

    public long getPrimaryFileSize() {
        return primaryFileSize;
    }

    public long getSecondaryFileSize() {
        return secondaryFileSize;
    }

    public long getTotalDownloadedSize() {
        return primaryDownloadedSize + secondaryDownloadedSize;
    }

    public long getTotalSize() {
        return primaryFileSize + secondaryFileSize;
    }

    public long getPrimaryDownloadedSize() {
        return primaryDownloadedSize;
    }

    public void setPrimaryDownloadedSize(long primaryDownloadedSize) {
        this.primaryDownloadedSize = primaryDownloadedSize;
    }

    public void setSecondaryDownloadedSize(long secondaryDownloadedSize) {
        this.secondaryDownloadedSize = secondaryDownloadedSize;
    }

    public long getSecondaryDownloadedSize() {
        return secondaryDownloadedSize;
    }

    public void setPrimaryStatus(int primaryStatus) {
        this.primaryStatus = primaryStatus;
    }

    public int getPrimaryStatus() {
        return primaryStatus;
    }

    public void setSecondaryStatus(int secondaryStatus) {
        this.secondaryStatus = secondaryStatus;
    }

    public int getSecondaryStatus() {
        return secondaryStatus;
    }

    public void setSelected(boolean selected) {
        isSelected = selected;
    }

    public boolean isSelected() {
        return isSelected;
    }
}
