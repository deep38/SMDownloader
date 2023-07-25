package com.ocean.smdownloader.Internet;

public interface SizeLoadListener {
    void onLoad(long size, Object object);
    void onFailed(String error, Object object);
}
