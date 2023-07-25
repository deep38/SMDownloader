package com.ocean.smdownloader.Download;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import androidx.annotation.Nullable;

public class DownloadTasksDatabase extends SQLiteOpenHelper {

    public static final String DATABASE_NAME = "SM_DOWNLOADER";
    public static final int VERSION = 1;

    public static final String TABLE_NAME = "DOWNLOAD_TASKS";
    public static final String COL_1 = "ID";
    public static final String COL_2 = "NAME";
    public static final String COL_3 = "TYPE";
    public static final String COL_4 = "SOURCE";
    public static final String COL_5 = "THUMBNAIL_PATH";
    public static final String COL_6 = "PRIMARY_LINK";
    public static final String COL_7 = "SECONDARY_LINK";
    public static final String COL_8 = "PRIMARY_FILE_PATH";
    public static final String COL_9 = "SECONDARY_FILE_PATH";
    public static final String COL_10 = "PRIIMARY_DOWNLOADED_SIZE";
    public static final String COL_11 = "SECONDARY_DOWNLOADED_SIZE";
    public static final String COL_12 = "PRIMARY_FILE_SIZE";
    public static final String COL_13 = "SECONDARY_FILE_SIZE";
    public static final String COL_14 = "PRIMARY_STATUS";
    public static final String COL_15 = "SECONDARY_STATUS";

    public DownloadTasksDatabase(@Nullable Context context) {
        super(context, DATABASE_NAME, null, VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + "(ID INTEGER PRIMARY KEY," +
                COL_2 + " TEXT," +
                COL_3 + " TEXT," +
                COL_4 + " TEXT," +
                COL_5 + " TEXT," +
                COL_6 + " TEXT," +
                COL_7 + " TEXT," +
                COL_8 + " TEXT," +
                COL_9 + " TEXT," +
                COL_10 + " LONG," +
                COL_11 + " LONG," +
                COL_12 + " LONG," +
                COL_13 + " LONG," +
                COL_14 + " INTEGER," +
                COL_15 + " INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
        sqLiteDatabase.execSQL("DROP TABLE IF EXISTS "+ TABLE_NAME);
        onCreate(sqLiteDatabase);
    }

    public boolean insertData(DownloadTaskData taskData){
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_1, taskData.getId());
        values.put(COL_2, taskData.getName());
        values.put(COL_3, taskData.getType());
        values.put(COL_4, taskData.getSource());
        values.put(COL_5, taskData.getThumbnailPath());
        values.put(COL_6, taskData.getPrimaryLink());
        values.put(COL_7, taskData.getSecondaryLink());
        values.put(COL_8, taskData.getPrimaryFilePath());
        values.put(COL_9, taskData.getSecondaryFilePath());
        values.put(COL_10, taskData.getPrimaryDownloadedSize());
        values.put(COL_11, taskData.getSecondaryDownloadedSize());
        values.put(COL_12, taskData.getPrimaryFileSize());
        values.put(COL_13, taskData.getSecondaryFileSize());
        values.put(COL_14, taskData.getPrimaryStatus());
        values.put(COL_15, taskData.getSecondaryStatus());

        long result = db.insert(TABLE_NAME, null, values);

        return result != -1;
    }

    public void updatePrimaryFilePath(int id, String path) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_8, path);

        db.update(TABLE_NAME, values, COL_1 + "=?", new String[]{String.valueOf(id)});
    }

    public void updatePrimaryDownloadedSize(int id, long downloadedSize) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_10, downloadedSize);

        db.update(TABLE_NAME, values, COL_1 + "=?", new String[]{String.valueOf(id)});
    }

    public void updateSecondaryDownloadedSize(int id, long downloadedSize) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_11, downloadedSize);

        db.update(TABLE_NAME, values, COL_1 + "=?", new String[]{String.valueOf(id)});
    }

    public void updatePrimaryStatus(int id, int status) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_14, status);

        db.update(TABLE_NAME, values, COL_1 + "=?", new String[]{String.valueOf(id)});
    }

    public void updateSecondaryStatus(int id, int status) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_15, status);

        db.update(TABLE_NAME, values, COL_1 + "=?", new String[]{String.valueOf(id)});
    }

    public Cursor getAllData() {
        return getWritableDatabase().rawQuery("SELECT * FROM " + TABLE_NAME, null);
    }

    public void deleteData(String name) {
        getWritableDatabase().execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + COL_2 + "='" + name +"'");
    }

    public void deleteData(int id) {
        getWritableDatabase().execSQL("DELETE FROM " + TABLE_NAME + " WHERE " + COL_1 + "=" + id +"");
    }
}
