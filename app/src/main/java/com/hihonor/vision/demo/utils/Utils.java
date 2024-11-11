/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

package com.hihonor.vision.demo.utils;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.hihonor.mcs.intelligence.visionkit.log.VisionLog;

import com.lcw.library.imagepicker.ImagePicker;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * 功能描述
 *
 * @since 2023-12-29
 */
public class Utils {
    private static final String TAG = "Utils";

    private static final Handler UI_HANDLER = new Handler(Looper.getMainLooper());

    private static final int REQUEST_EXTERNAL_STORAGE_IMAGE = 100;

    private static final int REQUEST_EXTERNAL_STORAGE_IMAGE_FOR_PHOTOS = 101;

    private static final int REQUEST_EXTERNAL_STORAGE_VIDEO = 102;

    private static final int RESULT_LOAD_IMAGE = 0;

    private static final int RESULT_LOAD_VIDEO = 1;

    private static final String[] PERMISSIONS_STORAGE =
        {"android.permission.READ_EXTERNAL_STORAGE", "android.permission.WRITE_EXTERNAL_STORAGE"};

    private static void requestStorePermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.S) {
            activity.requestPermissions(
                new String[] {Manifest.permission.ACCESS_MEDIA_LOCATION, "android.permission.READ_MEDIA_IMAGES"},
                requestCode);
        } else {
            activity.requestPermissions(
                new String[] {Manifest.permission.ACCESS_MEDIA_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                requestCode);
        }
    }

    public static void pickUpPhoto(AppCompatActivity activity) {
        if (!hasPermission(activity.getApplicationContext(), PERMISSIONS_STORAGE)) {
            requestStorePermission(activity, REQUEST_EXTERNAL_STORAGE_IMAGE);
        } else {
            openGallery(activity);
        }
    }

    /**
     * 选择多张图片
     *
     * @param activity {@link Activity}
     */
    public static void pickUpPhotos(AppCompatActivity activity) {
        if (!hasPermission(activity.getApplicationContext(), PERMISSIONS_STORAGE)) {
            requestStorePermission(activity, REQUEST_EXTERNAL_STORAGE_IMAGE_FOR_PHOTOS);
        } else {
            openImagePicker(activity);
        }
    }

    private static void openImagePicker(AppCompatActivity activity) {
        ImagePicker.getInstance()
            .setTitle("选择图片")// 设置标题
            .showCamera(false)// 设置是否显示拍照按钮
            .showImage(true)// 设置是否展示图片
            .showVideo(false)// 设置是否展示视频
            .filterGif(true)// 设置是否过滤gif图片
            .setMaxCount(500)// 设置最大选择图片数目(默认为1，单选)
            .setSingleType(true)// 设置图片视频不能同时选择
            .setImageLoader(new GlideLoader(activity))
            .start(activity, RESULT_LOAD_IMAGE);// REQEST_SELECT_IMAGES_CODE为Intent调用的requestCode\
    }

    /**
     * 选择视频
     *
     * @param activity {@link Activity}
     */
    public static void pickUpVideo(AppCompatActivity activity) {
        if (!hasPermission(activity.getApplicationContext(), PERMISSIONS_STORAGE)) {
            requestStorePermission(activity, REQUEST_EXTERNAL_STORAGE_VIDEO);
        } else {
            openGalleryForVideo(activity);
        }
    }

    private static void openVideoPicker(AppCompatActivity activity) {
        ImagePicker.getInstance()
            .setTitle("选择视频")// 设置标题
            .showCamera(false)// 设置是否显示拍照按钮
            .showImage(false)// 设置是否展示图片
            .showVideo(true)// 设置是否展示视频
            .filterGif(true)// 设置是否过滤gif图片
            .setMaxCount(1)// 设置最大选择视频数目(默认为1，单选)
            .setSingleType(true)// 设置图片视频不能同时选择
            .setImageLoader(new GlideLoader(activity))
            .start(activity, RESULT_LOAD_VIDEO);// REQEST_SELECT_IMAGES_CODE为Intent调用的requestCode\
    }

    /**
     * 权限检查方法，false代表没有该权限，ture代表有该权限
     *
     * @param context context
     * @param permissions 权限
     * @return 是否有权限
     */
    private static boolean hasPermission(Context context, String... permissions) {
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    /**
     * 打开图库
     *
     * @param activity context
     */
    private static void openGallery(AppCompatActivity activity) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setPackage("com.hihonor.photos");
        activity.startActivityForResult(intent, RESULT_LOAD_IMAGE);
    }

    /**
     * 打开图库-仅选择视频
     * 
     * @param activity context
     */
    private static void openGalleryForVideo(AppCompatActivity activity) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setPackage("com.hihonor.photos");
        intent.setType("video/*");
        activity.startActivityForResult(intent, RESULT_LOAD_VIDEO);
    }

    public static void onRequestPermissionsResult(int requestCode, AppCompatActivity activity,
        @NonNull int[] grantResults) {

        if (requestCode == REQUEST_EXTERNAL_STORAGE_IMAGE) {
            boolean result = Arrays.stream(grantResults).allMatch(value -> value == PackageManager.PERMISSION_GRANTED);
            if (result) {
                openGallery(activity);
            }
            return;
        }

        if (requestCode == REQUEST_EXTERNAL_STORAGE_IMAGE_FOR_PHOTOS) {
            boolean result = Arrays.stream(grantResults).allMatch(value -> value == PackageManager.PERMISSION_GRANTED);
            if (result) {
                openImagePicker(activity);
            }
            return;
        }

        if (requestCode == REQUEST_EXTERNAL_STORAGE_VIDEO) {
            boolean result = Arrays.stream(grantResults).allMatch(value -> value == PackageManager.PERMISSION_GRANTED);
            if (result) {
                openGalleryForVideo(activity);
            }
        }
    }

    public static List<Bitmap> onActivityResultForPhotos(int requestCode, int resultCode, Intent data) {
        List<Bitmap> list = new ArrayList<>();
        if (requestCode == RESULT_LOAD_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            ArrayList<String> stringArrayListExtra = data.getStringArrayListExtra(ImagePicker.EXTRA_SELECT_IMAGES);
            if (stringArrayListExtra != null && stringArrayListExtra.size() > 0) {
                stringArrayListExtra.forEach(s -> {
                    Bitmap bitmap = getBitmap(s);
                    if (bitmap != null) {
                        list.add(bitmap);
                    }
                });
            }
        }
        return list;
    }

    public static Bitmap getBitmap(String picturePath) {
        Bitmap bitmap = null;
        try {
            FileInputStream fis = new FileInputStream(picturePath);
            Bitmap tempBitmap = BitmapFactory.decodeStream(fis);
            if (tempBitmap != null) {
                //  0字节（0像素）文件、择图片格式损坏的文件：BitmapFactory.decodeStream(fis) == null
                bitmap = tempBitmap.copy(Bitmap.Config.ARGB_8888, true);
                if (ImageUtil.readPictureDegree(picturePath) != 0) {
                    bitmap = ImageUtil.rotateBitmapByDegree(bitmap, ImageUtil.readPictureDegree(picturePath));
                }
            }
        } catch (FileNotFoundException e) {
            VisionLog.error(TAG, "get bitmap error :" + e.getMessage());
        }
        return bitmap;
    }

    public static Bitmap onActivityResult(int requestCode, int resultCode, Intent data, AppCompatActivity activity,
        boolean isCompress) {
        if (requestCode != RESULT_LOAD_IMAGE || resultCode != Activity.RESULT_OK || data == null) {
            return null;
        }
        if (!isCompress) {
            return onActivityResult(requestCode, resultCode, data, activity);
        }
        VisionLog.info(TAG, "get compress bitmap");
        Uri selectedImage = data.getData();
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = activity.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        if (cursor == null) {
            return null;
        }
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();
        Optional<Bitmap> optionalBitmap = BitmapDecoder.decodeBitmap(picturePath);
        return optionalBitmap.orElse(null);
    }

    public static Bitmap onActivityResult(int requestCode, int resultCode, Intent data, AppCompatActivity activity) {
        if (requestCode != RESULT_LOAD_IMAGE || resultCode != Activity.RESULT_OK || data == null) {
            return null;
        }
        Uri selectedImage = data.getData();
        String[] filePathColumn = {MediaStore.Images.Media.DATA};
        Cursor cursor = activity.getContentResolver().query(selectedImage, filePathColumn, null, null, null);
        cursor.moveToFirst();
        int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
        String picturePath = cursor.getString(columnIndex);
        cursor.close();
        try {
            FileInputStream fis = new FileInputStream(picturePath);
            Bitmap tempBitmap = BitmapFactory.decodeStream(fis);
            if (tempBitmap != null) { // 0字节（0像素）文件、择图片格式损坏的文件：BitmapFactory.decodeStream(fis) == null
                Bitmap bitmap = tempBitmap.copy(Bitmap.Config.ARGB_8888, true);
                if (ImageUtil.readPictureDegree(picturePath) != 0) {
                    bitmap = ImageUtil.rotateBitmapByDegree(bitmap, ImageUtil.readPictureDegree(picturePath));
                }
                return bitmap;
            }
        } catch (FileNotFoundException e) {
            VisionLog.error(TAG, "get bitmap error :" + e.getMessage());
        }
        return null;
    }

    public static Uri onActivityForUriResult(int requestCode, int resultCode, Intent data) {
        if (requestCode != RESULT_LOAD_VIDEO || resultCode != Activity.RESULT_OK || data == null) {
            return null;
        }
        return data.getData();
    }

    /**
     * toast 弹窗
     *
     * @param context {@link Context}
     * @param msg message
     */
    public static void showToast(Context context, String msg) {
        runUiThread(() -> Toast.makeText(context, msg, Toast.LENGTH_LONG).show());

    }

    /**
     * 主线程运行工具函数
     *
     * @param runnable {@Runnable}
     */
    public static void runUiThread(Runnable runnable) {
        if (Thread.currentThread() == Looper.getMainLooper().getThread()) {
            runnable.run();
        } else {
            UI_HANDLER.post(runnable::run);
        }
    }

    /**
     * 检查是否有读写SD卡的权限
     *
     * @param context {@link Context}
     * @return true ： 有 ； false ：没有
     */
    public static boolean checkSdPermission(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return Environment.isExternalStorageManager();
        } else {
            return hasPermission(context, PERMISSIONS_STORAGE);
        }
    }

    /**
     * 请求SD 卡的访问权限
     *
     * @param activity {@link Activity}
     * @param requestCode 请求码
     */
    public static void requestSdPermission(Activity activity, int requestCode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivityForResult(intent, requestCode);
        } else {
            activity.requestPermissions(
                new String[] {Manifest.permission.ACCESS_MEDIA_LOCATION, Manifest.permission.WRITE_EXTERNAL_STORAGE},
                requestCode);
        }
    }
}
