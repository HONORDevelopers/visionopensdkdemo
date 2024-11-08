/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

package com.hihonor.vision.demo.focusocr;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.hihonor.mcs.intelligence.vision.common.exception.VisionResultException;
import com.hihonor.mcs.intelligence.vision.ocr.VisionTextDetector;
import com.hihonor.mcs.intelligence.visionkit.data.BundleKey;
import com.hihonor.mcs.intelligence.visionkit.data.VisionImage;
import com.hihonor.mcs.intelligence.visionkit.data.VisionResult;
import com.hihonor.mcs.intelligence.visionkit.data.text.Text;
import com.hihonor.mcs.intelligence.visionkit.data.text.TextConstant;
import com.hihonor.mcs.intelligence.visionkit.log.VisionLog;
import com.hihonor.vision.demo.utils.ImageUtil;
import com.hihonor.vision.demo.utils.Utils;
import com.hihonor.visiondemo.R;
import com.hihonor.visiondemo.databinding.ActivityFocusocrBinding;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Focus OCR 测试demo
 *
 * @since 2024-01-17
 */
@SuppressLint("RestrictedApi")
public class FocusOcrActivity extends AppCompatActivity {
    private static final String TAG = "FocusOcrActivity";

    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    private long mTime;

    private VisionTextDetector textDetector;

    private int keepAliveTime = 10 * 60 * 1000;

    private Bitmap bitmap;

    private ActivityFocusocrBinding mBinding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityFocusocrBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mExecutorService.execute(() -> bitmap = ImageUtil.getBitmap(this, R.drawable.default_face));
        init();
    }

    @SuppressLint("SetTextI18n")
    private void init() {
        mBinding.btnSelectImage.setOnClickListener(v -> Utils.pickUpPhoto(FocusOcrActivity.this));

        mBinding.btnConnService.setOnClickListener(v -> {
            resetStatus();
            getClient();
            open();
        });
        mBinding.btnDisconnectService.setOnClickListener(v -> {
            resetStatus();
            close();
        });
        mBinding.btnDetect.setOnClickListener(v -> {
            resetStatus();
            detect();
        });
    }

    private void getClient() {
        mExecutorService.submit(() -> {
            if (textDetector == null) {
                try {
                    textDetector = VisionTextDetector.getClient(getApplicationContext(), keepAliveTime, "visionDemo",
                        new HashMap<>()); // 不填默认最新
                } catch (VisionResultException e) {
                    VisionLog.error(TAG, "VisionResultException", e);
                }
            }
        });
    }

    private void open() {
        mExecutorService.submit(() -> {
            if (textDetector == null) {
                VisionLog.info(TAG, "textDetector is null");
                return;
            }
            mTime = SystemClock.elapsedRealtime();
            int code = textDetector.open();
            VisionLog.info(TAG, "open code = " + code);
            String spendTime = (SystemClock.elapsedRealtime() - mTime) + "ms";
            runOnUiThread(() -> {
                Toast.makeText(FocusOcrActivity.this, "插件加载成功！", Toast.LENGTH_LONG).show();
                mBinding.tvResult.append("模型加载时延：" + spendTime + ", resultCode = " + code + "\n");
            });
        });
    }

    private void detect() {
        if (bitmap == null) {
            VisionLog.debug(TAG, "bitmap is null");
            return;
        }
        VisionImage visionImage = VisionImage.fromBitmap(bitmap);

        mTime = SystemClock.elapsedRealtime();
        long startTime = SystemClock.elapsedRealtime();
        getClient();
        open();
        mExecutorService.submit(() -> {
            Bundle bundle = new Bundle();
            bundle.putInt(BundleKey.TEXT_CONFIG_LANGUAGE, TextConstant.Language.AUTO);
            bundle.putInt(BundleKey.TEXT_CONFIG_SHAPE, TextConstant.TextShape.CURVE);
            bundle.putInt(BundleKey.TEXT_CONFIG_DETECT_MODE, TextConstant.DetectMode.OCR_MODE);
            if (textDetector == null) {
                VisionLog.info(TAG, "textDetector is null");
                return;
            }
            VisionResult<Text> visionResult = textDetector.detect(visionImage, bundle);
            if (visionResult != null && visionResult.getResult() != null) {
                showTextResult(visionResult, SystemClock.elapsedRealtime() - startTime);
            }
        });
    }

    private void close() {
        mExecutorService.submit(() -> {
            if (textDetector != null) {
                textDetector.close();
                textDetector = null;
            }
        });
    }

    private void resetStatus() {
        mBinding.tvResult.setText("");
    }

    @Override
    protected void onDestroy() {
        VisionLog.info(TAG, "onDestroy");
        mExecutorService.execute(this::close);
        super.onDestroy();
    }

    @SuppressLint("RestrictedApi")
    private void showTextResult(VisionResult<Text> result, long costTime) {
        runOnUiThread(() -> mBinding.tvResult.append("模型运行时延：" + costTime + "ms, resultCode = " + result.getResultCode()
            + ", resultResult = " + result.getResult().getValue() + "\n"));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mBinding.acIvImage.setVisibility(View.VISIBLE);
        bitmap = Utils.onActivityResult(requestCode, resultCode, data, this);
        mBinding.acIvImage.setImageBitmap(bitmap);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
        @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Utils.onRequestPermissionsResult(requestCode, this, grantResults);
    }
}