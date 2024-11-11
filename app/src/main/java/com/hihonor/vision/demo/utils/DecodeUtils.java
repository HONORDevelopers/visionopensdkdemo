/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

package com.hihonor.vision.demo.utils;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.graphics.Canvas;
import android.graphics.ColorSpace;
import android.graphics.Paint;
import android.os.Handler;

import com.hihonor.mcs.intelligence.visionkit.log.VisionLog;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Optional;


/**
 * 解码工具类
 *
 * @author z00311402
 * @since 2017-07-06
 */
@SuppressLint("RestrictedApi")
public class DecodeUtils {
    private static final String TAG = "DecodeUtils";

    private static final int MAX_INITIAL_SIZE = 8;

    private static final int FACTOR = 2;

    private static final int DECODE_QUALITY = 100;

    private static String sPreviousImageName = null;

    private static Bitmap sPreviousImage = null;

    /**
     * FWK接口，解码时要求FWK是什么色域返回什么色域
     */
    private static final int KEEP_ORIGINAL_COLOR_SPACE = 101102;

    private DecodeUtils() {
    }

    /**
     * 根据size获取bitmap缩略图
     *
     * @param fd 文件描述符
     * @param options options
     * @param minSize 最小size
     * @param maxSize 最大size
     * @param classifyPicScalingFactor 分类图片裁剪参数
     * @return bitmap的optional结果
     */

    public static Optional<Bitmap> getBitmapByMinSize(FileDescriptor fd, Options options, int minSize, int maxSize,
        float classifyPicScalingFactor) {
        Options newOptions = options;
        if (newOptions == null) {
            newOptions = new Options();
        }
        newOptions.inJustDecodeBounds = true;
        newOptions.outHeight = KEEP_ORIGINAL_COLOR_SPACE;
        BitmapFactory.decodeFileDescriptor(fd, null, newOptions);

        int destWidth = newOptions.outWidth;
        int destHeight = newOptions.outHeight;
        float scale = 1;

        int minWidHeigh = Math.min(destWidth, destHeight);
        if ((minWidHeigh >= (FACTOR * minSize)) && (classifyPicScalingFactor < 1)) {
            destWidth = (int) (newOptions.outWidth * classifyPicScalingFactor);
            destHeight = (int) (newOptions.outHeight * classifyPicScalingFactor);
            scale = classifyPicScalingFactor;

            if ((destWidth < minSize) || (destHeight < minSize)) {
                destWidth *= FACTOR;
                destHeight *= FACTOR;
                scale *= FACTOR;
            }
            if ((destWidth < minSize) || (destHeight < minSize)) {
                scale = 1;
            }
        }
        newOptions.inSampleSize = computeSampleSizeLarger(scale); // */scale
        newOptions.inJustDecodeBounds = false;
        setOptionsMutable(newOptions);
        newOptions.outHeight = KEEP_ORIGINAL_COLOR_SPACE;
        Bitmap result = BitmapFactory.decodeFileDescriptor(fd, null, newOptions);

        if (result == null) {
            VisionLog.error(TAG, " ClassifyService ---decodeFileDescriptor fail ! ");
            /* 返回Optional影响性能 */
            return Optional.empty();
        }

        float scale1 = (float) maxSize / Math.max(result.getWidth(), result.getHeight());
        if ((scale1 < 1) && (classifyPicScalingFactor != 1)) {
            result = resizeBitmapByScale(result, scale1, true);
        }
        return Optional.of(result);
    }

    /**
     * 根据size获取bitmap缩略图
     *
     * @param minSize 最小size
     * @param maxSize 最大size
     * @param classifyPicScalingFactor 分类图片裁剪参数
     * @param srcWidth src image width
     * @param srcHeight src image height
     * @return bitmap的optional结果
     */
    public static int[] getBitmapScaleByMinSize(int minSize, int maxSize, float classifyPicScalingFactor, int srcWidth,
        int srcHeight) {
        Options newOptions = new Options();
        newOptions.inJustDecodeBounds = true;
        newOptions.outWidth = srcWidth;
        newOptions.outHeight = srcHeight;
        int destWidth = srcWidth;
        int destHeight = srcHeight;
        float scale = 1;
        int minWidHeigh = Math.min(destWidth, destHeight);
        if ((minWidHeigh >= (FACTOR * minSize)) && (classifyPicScalingFactor < 1)) {
            destWidth = (int) (newOptions.outWidth * classifyPicScalingFactor);
            destHeight = (int) (newOptions.outHeight * classifyPicScalingFactor);
            scale = classifyPicScalingFactor;
            if ((destWidth < minSize) || (destHeight < minSize)) {
                destWidth *= FACTOR;
                destHeight *= FACTOR;
                scale *= FACTOR;
            }
            if ((destWidth < minSize) || (destHeight < minSize)) {
                scale = 1;
            }
        }
        newOptions.inSampleSize = computeSampleSizeLarger(scale); // */scale
        newOptions.outHeight = KEEP_ORIGINAL_COLOR_SPACE;
        if (newOptions.inSampleSize > 1) {
            newOptions.outWidth = Math.round(1.0f * newOptions.outWidth / newOptions.inSampleSize);
            newOptions.outHeight = Math.round(1.0f * newOptions.outHeight / newOptions.inSampleSize);
        }
        int[] scaleSize = {newOptions.outWidth, newOptions.outHeight};
        float scale1 = (float) maxSize / Math.max(newOptions.outWidth, newOptions.outHeight);
        if ((scale1 < 1) && (classifyPicScalingFactor != 1)) {
            int width = Math.round(scaleSize[0] * scale);
            int height = Math.round(scaleSize[1] * scale);
            if (width != newOptions.outWidth || height != newOptions.outHeight) {
                scaleSize[0] = Math.round(width * scale);
                scaleSize[1] = Math.round(height * scale);
            }
        }
        return scaleSize;
    }

    /**
     * 获取缩略图
     *
     * @param result bitmap
     * @param name 文件名
     * @param cacheDir 缓存目录
     * @param handler handler
     * @return bitmap的optional结果
     */
    public static Optional<Bitmap> getBitmapByMaxSize(Bitmap result, String name, File cacheDir, Handler handler) {
        /**
         * Compress previous image, avoid the case that algo and jpeg both process one image at the same time
         */
        if (handler == null) {
            VisionLog.info(TAG, "getBitmapByMaxSize invalid handler.");
            return Optional.empty();
        }
        if (sPreviousImageName != null && sPreviousImage != null) {
            File cacheFile = new File(cacheDir, sPreviousImageName);
            VisionLog.info(TAG, "{CacheFileExist:" + cacheFile.exists() + "}");
            if (!cacheFile.exists()) {
                FileOutputStream outputStream;
                try {
                    outputStream = new FileOutputStream(cacheFile);
                    handler.post(new CacheFile(sPreviousImage, outputStream)); // close the outputStream in thread.
                } catch (FileNotFoundException e) {
                    VisionLog.error(TAG, "file not found");
                }
            }
        }
        sPreviousImageName = name;
        sPreviousImage = result;
        return Optional.of(result);
    }

    private static void setOptionsMutable(Options options) {
        options.inMutable = true;
    }

    private static int computeSampleSizeLarger(float scale) {
        // 此处不需要特别精确计算, 直接使用float和double
        int initialSize = (int) Math.floor(1d / scale);
        if (initialSize <= 1) {
            return 1;
        }
        return initialSize <= MAX_INITIAL_SIZE ? prevPowerOf2(initialSize)
            : initialSize / MAX_INITIAL_SIZE * MAX_INITIAL_SIZE;
    }

    private static int prevPowerOf2(int value) {
        if (value <= 0) {
            throw new IllegalArgumentException();
        }
        return Integer.highestOneBit(value);
    }

    private static Bitmap resizeBitmapByScale(Bitmap bitmap, float scale, boolean isRecycle) {
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        if (width == bitmap.getWidth() && height == bitmap.getHeight()) {
            return bitmap;
        }
        Bitmap target = Bitmap.createBitmap(width, height, getConfig(bitmap), true, getColorSpace(bitmap));
        Canvas canvas = new Canvas(target);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        if (isRecycle) {
            bitmap.recycle();
        }
        return target;
    }

    /**
     * 获取Bitmap的色域
     *
     * @param bitmap 输入的bitmap
     * @return 色域
     */
    public static ColorSpace getColorSpace(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return ColorSpace.get(ColorSpace.Named.SRGB);
        }
        ColorSpace colorSpace = bitmap.getColorSpace();
        if (colorSpace == null || colorSpace.getId() == ColorSpace.MIN_ID) {
            colorSpace = ColorSpace.get(ColorSpace.Named.SRGB);
        }
        return colorSpace;
    }
    
    private static Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }
        return config;
    }

    /**
     * 内部类，文件缓存
     *
     * @author c00532423
     * @since 2019-11-01
     */
    private static class CacheFile implements Runnable {
        private Bitmap mBitmap;

        private OutputStream mOutputStream;

        CacheFile(Bitmap bitmap, OutputStream outputStream) {
            mBitmap = bitmap;
            mOutputStream = outputStream;
        }

        @Override
        public void run() {
            mBitmap.compress(Bitmap.CompressFormat.JPEG, DECODE_QUALITY, mOutputStream);
        }
    }
}
