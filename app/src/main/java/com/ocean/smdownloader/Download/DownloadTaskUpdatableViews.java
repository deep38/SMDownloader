package com.ocean.smdownloader.Download;

import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

public class DownloadTaskUpdatableViews {

    private final ImageButton imageButton;
    private final TextView sizeTextView;
    private final TextView progressTextView;
    private final ProgressBar progressBar;

    public DownloadTaskUpdatableViews(ImageButton imageButton, TextView sizeTextView, TextView progressTextView, ProgressBar progressBar) {
        this.imageButton = imageButton;
        this.sizeTextView = sizeTextView;
        this.progressTextView = progressTextView;
        this.progressBar = progressBar;
    }

    public ImageButton getImageButton() {
        return imageButton;
    }

    public TextView getSizeTextView() {
        return sizeTextView;
    }

    public TextView getProgressTextView() {
        return progressTextView;
    }

    public ProgressBar getProgressBar() {
        return progressBar;
    }
}
