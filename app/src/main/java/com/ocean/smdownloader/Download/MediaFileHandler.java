package com.ocean.smdownloader.Download;

import android.app.AlertDialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.ocean.smdownloader.R;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public class MediaFileHandler {
    private final String NO_MEDIA_FILE_NAME = "nomedia";
    private final String TYPE_NO_MEDIA = "application/x-nomedia";
    public static final String TYPE_AUDIO = "audio/mpeg";
    public static final String TYPE_VIDEO = "video/mp4";
    public static final String TYPE_IMAGE = "image/jpeg";

    private final Context context;
    private final MediaFileHandlerListener listener;
    private final ContentResolver resolver;

    public MediaFileHandler(Context context,  MediaFileHandlerListener listener) {
        this.context = context;
        this.listener = listener;

        resolver = context.getContentResolver();
    }

    public String getPathForFile(String name, String type, String dir, boolean hasSecondaryFile, @NonNull long... size) {

        String mediaDir = dir == null ? getMediaFolder(type) : dir;
//        Log.d("MediaFileHandler", "First init: " + downloadDir);
        File mediaFile = new File(mediaDir + File.separator + name + getExtension(type));
        String downloadDir = dir == null ? getDir(type, name) : dir;
        downloadDir = dir == null && hasSecondaryFile ? downloadDir + File.separator + name : downloadDir;
        Log.d("MediaFileHandler", "Second init: " + downloadDir);

        if (!mediaFile.exists()) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (TYPE_VIDEO.equals(type)) {
                        String filePath = createFile(name, downloadDir, type);
                        RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "rw");
                        randomAccessFile.setLength(size[0]);
                        randomAccessFile.close();

                        if (hasSecondaryFile) {
                            new RandomAccessFile(new File(Environment.getExternalStorageDirectory() + File.separator + downloadDir + File.separator + ".nomedia"), "rw").close();
                        }

                        return filePath;

                    } else {
                        if (dir == null) {
                            return createFile(name, downloadDir, type);
                        } else {
                            String filePath = Environment.getExternalStorageDirectory() + File.separator + downloadDir + File.separator + name + getExtension(type);
                            RandomAccessFile newMediaFile = new RandomAccessFile(filePath, "rw");
                            newMediaFile.setLength(size[0]);
                            newMediaFile.close();

                            return filePath;
                        }
                    }
                } else {
                    File folder = new File(Environment.getExternalStorageDirectory() + File.separator + getDir(type, name));
                    if (folder.exists() || folder.mkdirs()) {
                        String filePath = folder.getAbsolutePath() + File.separator + name + getExtension(type);
                        if (TYPE_VIDEO.equals(type)) {
                            File noMediaFile = new File(folder.getAbsolutePath() + File.separator + ".nomedia");
                            noMediaFile.createNewFile();
                        }
                        RandomAccessFile randomAccessFile = new RandomAccessFile(filePath, "rw");
                        randomAccessFile.setLength(size[0]);
                        randomAccessFile.close();

                        return filePath;
                    } else {
                        return null;
                    }
                }
            } catch (IOException e) {
                showMessage(e.getMessage());
            }
        } else {
            showFileExistAlert(name, type, hasSecondaryFile, size);
        }
        return null;
    }

    @RequiresApi(api = Build.VERSION_CODES.Q)
    private String createFile(String fileName, String dir, String type) throws IOException {
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, fileName);
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, type);
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, dir);

        Uri fileUri = resolver.insert(getUri(type), contentValues);
        Cursor cursor = resolver.query(fileUri, new String[]{MediaStore.MediaColumns.DATA}, null, null, null);

        String filePath = null;
        if (cursor.getCount() > 0) {
            cursor.moveToFirst();
            filePath = cursor.getString(0);
        }
        cursor.close();

        return filePath;
    }

    private void showFileExistAlert(String name, String type, boolean hasSecondaryFile, long... size) {
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Download")
                .setMessage(name + getExtension(type) + " file already exist.")
                .setPositiveButton("Save as new", (dialogInterface, i) -> showFileNameDialog(name, type, hasSecondaryFile, size))
                .setNegativeButton("Overwrite", (dialogInterface, i) -> overWriteFile(name, type, hasSecondaryFile, size))
                .setNeutralButton("Cancel", (dialogInterface, i) -> listener.onCanceled())
                .setOnCancelListener(dialogInterface -> listener.onCanceled())
                .create();

        dialog.show();

    }

    private void showFileNameDialog(String name, String type, boolean hasSecondaryFile, long... size) {
        View dialogView = context.getSystemService(LayoutInflater.class).inflate(R.layout.dialog_edit_text_layout, null);
        AlertDialog dialog = new AlertDialog.Builder(context)
                .setTitle("Enter New File Name")
                .setView(dialogView)
                .setPositiveButton("OK", null)
                .setNegativeButton("CANCEL", (dialogInterface, i) -> listener.onCanceled())
                .setOnCancelListener(dialogInterface -> listener.onCanceled())
                .create();

        dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);

        dialog.show();

        Button okButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        EditText nameEditText = dialogView.findViewById(R.id.downloadLinkEditText);
        nameEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
        nameEditText.setText(name + "__1");
        nameEditText.requestFocus();

        nameEditText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                onNameEditingDone(nameEditText.getText().toString(), name, type, hasSecondaryFile, dialog, size);
                return true;
            }
            return false;
        });

        okButton.setOnClickListener(view -> {
            onNameEditingDone(nameEditText.getText().toString(), name, type, hasSecondaryFile, dialog, size);
        });
    }

    private void onNameEditingDone(String editedName, String name, String type, boolean hasSecondaryFile, AlertDialog dialog, long... size) {
        if (editedName.equals("")) {
            showMessage("Please enter the name");
        } else if (editedName.equals(name)) {
            showMessage("Name must be different from current name");
        } else if ((new File(getMediaFolder(type) + File.separator + editedName + getExtension(type))).exists()) {
            showMessage("File already exist please change the name.");
        } else {
            createFile(editedName, type, hasSecondaryFile, size);
            dialog.dismiss();
        }
    }

    private void createFile(String name, String type, boolean hasSecondaryFile, long... size) {
        if (TYPE_VIDEO.equals(type)) {
            if (size != null && size.length >= 2) {
                String primaryFilePath = getPathForFile(name, type, null, hasSecondaryFile, size[0]);

                listener.onFileCreated(primaryFilePath, getPathForFile(name, TYPE_AUDIO, getSecondaryDir(primaryFilePath), hasSecondaryFile, size[1]), false);
            } else {
                listener.onError(name, "Error: Size not given for create file");
            }
        } else {
            if (size != null && size.length >= 1) {
                listener.onFileCreated(getPathForFile(name, type, null, hasSecondaryFile, size[0]), null, false);
            } else {
                listener.onError(name, "Error: Size not given for create file");
            }
        }
    }

    private void overWriteFile(String name, String type, boolean hasSecondaryFile, long... size) {
        if (TYPE_VIDEO.equals(type)) {
            if (size != null && size.length >= 2) {
                File file = new File(getMediaFolder(type) + File.separator + name + getExtension(type));

                file.delete();

                String primaryFilePath = getPathForFile(name, type, null, hasSecondaryFile, size[0]);
                listener.onFileCreated(primaryFilePath, getPathForFile(name, TYPE_AUDIO, getSecondaryDir(primaryFilePath), hasSecondaryFile, size[1]), true);
            } else {
                listener.onError(name, "Error: Size not given for create file");
            }
        } else {
            if (size != null && size.length >= 1) {
                File file = new File(getMediaFolder(type) + File.separator + name + getExtension(type));
                if (file.delete()) {
                    listener.onFileCreated(getPathForFile(name, type, null, hasSecondaryFile, size[0]), null, true);
                } else {
                    listener.onError(name, "Can't able to delete file: " + file.getAbsolutePath());
                }
            } else {
                listener.onError(name, "Error: Size not given for create file");
            }
        }
    }

    private void showMessage(String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    public static String getSupportedName(String name) {
        return name.replaceAll("[.,'!@#$%&*-+=|\\\\/?<>;:`~\\\"\\-\\(\\)\\{\\}\\[\\]]", " ").replaceAll("[ ]+", " ");
    }

    private String getDir(String type, String name) {
        switch (type) {
            case TYPE_AUDIO:
                return Environment.DIRECTORY_MUSIC + File.separator + context.getString(R.string.app_name);
            case TYPE_VIDEO:
                return Environment.DIRECTORY_MOVIES + File.separator + context.getString(R.string.app_name);
            case TYPE_IMAGE:
                return Environment.DIRECTORY_PICTURES +  File.separator + context.getString(R.string.app_name);
            default:
                return Environment.DIRECTORY_DOWNLOADS +  File.separator + context.getString(R.string.app_name);
        }
    }

    private String getMediaFolder(String type) {
        switch (type) {
            case TYPE_AUDIO:
                return Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_MUSIC + File.separator + context.getString(R.string.app_name);
            case TYPE_VIDEO:
                return Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_MOVIES + File.separator + context.getString(R.string.app_name);
            case TYPE_IMAGE:
                return Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_PICTURES +  File.separator + context.getString(R.string.app_name);
            default:
                return Environment.getExternalStorageDirectory() + File.separator + Environment.DIRECTORY_DOWNLOADS +  File.separator + context.getString(R.string.app_name);
        }
    }

    public static String getSecondaryDir(String primaryFilePath) {
        primaryFilePath = primaryFilePath.replace(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator, "");
        return primaryFilePath.substring(0, primaryFilePath.lastIndexOf(File.separator));
    }

    private Uri getUri(String type) {
        switch (type) {
            case TYPE_AUDIO:
                return MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
            case TYPE_VIDEO:
                return MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
            case TYPE_IMAGE:
                return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            default:
                return MediaStore.Files.getContentUri("external");
        }
    }

    public static String getExtension(String type) {
        switch (type) {
            case TYPE_AUDIO:
                return ".mp3";
            case TYPE_VIDEO:
                return ".mp4";
            case TYPE_IMAGE:
                return ".jpg";
            default:
                return "";
        }
    }
}
