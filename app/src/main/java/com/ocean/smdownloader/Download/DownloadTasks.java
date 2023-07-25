package com.ocean.smdownloader.Download;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;

public class DownloadTasks {
    public static int newAddedTasks = 0;
    public static int downloadedStartIndex = 0;
    public static ArrayList<DownloadTaskData> list = new ArrayList<>();

    public static boolean removeByName(String name) {
        for (DownloadTaskData data : list) {
            if (data.getName().equals(name)) {
                list.remove(data);

                return true;
            }
        }

        return false;
    }
}
