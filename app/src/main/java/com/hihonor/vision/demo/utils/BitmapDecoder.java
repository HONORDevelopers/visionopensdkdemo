/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

package com.hihonor.vision.demo.utils;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.text.TextUtils;

import com.hihonor.mcs.intelligence.visionkit.log.VisionLog;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;

/**
 * Bitmap 解码器
 *
 * @author t00335338
 * @since 2017-11-06
 */
@SuppressLint("RestrictedApi")
public class BitmapDecoder {
    private static final String TAG = "BitmapDecoder";

    private static final int CLASSIFY_MIN_SIZE = 1080;

    private static final int CLASSIFY_MAX_SIZE = 2560;

    private static final float CLASSIFY_PIC_SCALING = 0.25f;

    /**
     * Bitmap 解码器
     *
     * @param context Context
     */
    private BitmapDecoder(Context context) {
    }

    /**
     * 引入图库decode bitmap 方式
     *
     * @param filePath file path
     * @return Optional
     */
    public static Optional<Bitmap> decodeBitmap(String filePath) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        if (TextUtils.isEmpty(filePath)) {
            return Optional.empty();
        }
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(filePath);
            FileDescriptor fd = fis.getFD();
            Optional<Bitmap> optionalBitmap =
                DecodeUtils.getBitmapByMinSize(fd, options, CLASSIFY_MIN_SIZE, CLASSIFY_MAX_SIZE, CLASSIFY_PIC_SCALING);
            int degree = ImageUtil.readPictureDegree(filePath);
            if (degree != 0 && optionalBitmap.isPresent()) {
                Bitmap bitmap = ImageUtil.rotateBitmapByDegree(optionalBitmap.get(), degree);
                return Optional.of(bitmap);
            }
            return optionalBitmap;
        } catch (FileNotFoundException e) {
            VisionLog.error(TAG, "file not found");
            return Optional.empty();
        } catch (IOException e) {
            VisionLog.error(TAG, "IOException " + e.getMessage());
            return Optional.empty();
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    VisionLog.error(TAG, "IOException " + e.getMessage());
                }
            }
        }
    }

    /**
     * 获取生成人像时使用的缩率图的尺寸
     *
     * @param srcWidth image width
     * @param srcHeight image height
     * @return 缩率图的尺寸
     */
    public static int[] getFaceSampleBitmapSize(int srcWidth, int srcHeight) {
        return DecodeUtils.getBitmapScaleByMinSize(CLASSIFY_MIN_SIZE, CLASSIFY_MAX_SIZE, CLASSIFY_PIC_SCALING, srcWidth,
            srcHeight);
    }
}
