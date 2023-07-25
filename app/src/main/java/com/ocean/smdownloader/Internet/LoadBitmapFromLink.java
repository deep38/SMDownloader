package com.ocean.smdownloader.Internet;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.ImageView;

import java.io.InputStream;
import java.net.URL;

public class LoadBitmapFromLink extends  AsyncTask<String, String, Bitmap> {

    private BitmapLoadListener listener;
    private Object object;
    private ImageView imageView;

    public LoadBitmapFromLink(BitmapLoadListener listener, Object object) {
        this.listener = listener;
        this.object = object;
    }

    public LoadBitmapFromLink into(ImageView imageView) {
        this.imageView = imageView;
        return this;
    }

    @Override
    protected Bitmap doInBackground(String[] params) {
        Bitmap mThumbnail = null;
        try {
            URL url = new URL(params[0]);

            if (!isCancelled()) {
                InputStream in = url.openStream();
                if (!isCancelled())
                    mThumbnail = BitmapFactory.decodeStream(in);
            }
        } catch (Exception e) {
            Log.e("LoadingBitmap", e.getMessage());
        }

        return mThumbnail;
    }

    @Override
    protected void onPostExecute(Bitmap result) {
        super.onPostExecute(result);
        if (listener != null)
            listener.onBitmapLoaded(result, object);
        if (imageView != null)
            imageView.setImageBitmap(result);

    }

}