/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

package com.hihonor.vision.demo.docscan;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.TextUtils;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.hihonor.mcs.intelligence.vision.common.exception.VisionResultException;
import com.hihonor.mcs.intelligence.vision.docscan.VisionDocScanner;
import com.hihonor.mcs.intelligence.visionkit.api.ExtraInfoKey;
import com.hihonor.mcs.intelligence.visionkit.data.VisionImage;
import com.hihonor.mcs.intelligence.visionkit.data.VisionResult;
import com.hihonor.mcs.intelligence.visionkit.data.docscan.DocScanCoords;
import com.hihonor.mcs.intelligence.visionkit.data.docscan.DocScanFilter;
import com.hihonor.mcs.intelligence.visionkit.data.docscan.DocScanImage;
import com.hihonor.mcs.intelligence.visionkit.data.docscan.DocScanResults;
import com.hihonor.mcs.intelligence.visionkit.data.docscan.DocWarpPara;
import com.hihonor.mcs.intelligence.visionkit.log.VisionLog;
import com.hihonor.vision.demo.utils.ImageUtil;
import com.hihonor.vision.demo.utils.Utils;
import com.hihonor.visiondemo.R;
import com.hihonor.visiondemo.databinding.ActivityDocScanBinding;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 文档扫描
 * 
 * @since 2024-01-17
 */
@SuppressLint("RestrictedApi")
public class DocScanActivity extends AppCompatActivity {
    private static final String TAG = "DocScanActivity";

    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    private long mTime;

    private VisionDocScanner mDocScanner;

    private int keepAliveTime = 10 * 60 * 1000;

    private Bitmap bitmap;

    private ActivityDocScanBinding mBinding;

    private DocScanResults mResult;

    private DocScanImage mDocScanImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mBinding = ActivityDocScanBinding.inflate(getLayoutInflater());
        setContentView(mBinding.getRoot());
        mExecutorService.execute(() -> bitmap = ImageUtil.getBitmap(this, R.drawable.default_face));
        init();
    }

    @SuppressLint("SetTextI18n")
    private void init() {
        mBinding.matchSelectPhotoBtn.setOnClickListener(v -> Utils.pickUpPhoto(DocScanActivity.this));

        mBinding.connServiceBtn.setOnClickListener(v -> {
            resetStatus();
            getClient();
            open();
        });
        mBinding.disConnServiceBtn.setOnClickListener(v -> {
            resetStatus();
            close();
        });
        mBinding.docScan.setOnClickListener(v -> {
            resetStatus();
            docScan();
        });
        mBinding.docWarp.setOnClickListener(v -> {
            resetStatus();
            runOnUiThread(() -> mBinding.resultImage.setImageBitmap(null));
            docWarp();
        });
        mBinding.docFilter.setOnClickListener(v -> {
            resetStatus();
            runOnUiThread(() -> mBinding.resultImage.setImageBitmap(null));
            docFilter();
        });
        mBinding.docEnd.setOnClickListener(v -> {
            resetStatus();
            docEnd();
        });
    }

    private void getClient() {
        mExecutorService.submit(() -> {
            if (mDocScanner == null) {
                HashMap<Object, Object> extraInfo = new HashMap<>();
                extraInfo.put(ExtraInfoKey.BUSINESS_NAME, "visionDemo");
                try {
                    mDocScanner =
                        VisionDocScanner.getClient(getApplicationContext(), keepAliveTime, "visionDemo", extraInfo); //
                } catch (VisionResultException e) {
                    VisionLog.error(TAG, "VisionResultException", e);
                }
                // 不填默认最新
            }
        });
    }

    private void open() {
        mExecutorService.submit(() -> {
            if (mDocScanner == null) {
                VisionLog.info(TAG, "mDocScanner is null");
                return;
            }
            mTime = SystemClock.elapsedRealtime();
            int code = mDocScanner.open();
            VisionLog.info(TAG, "open code = " + code);
            String spendTime = (SystemClock.elapsedRealtime() - mTime) + "ms";
            mExecutorService.submit(() -> runOnUiThread(() -> {
                Toast.makeText(DocScanActivity.this, "插件加载成功！", Toast.LENGTH_LONG).show();
                mBinding.matchResultTextView.append("模型加载时延：" + spendTime + ", resultCode = " + code + "\n");
            }));
        });
    }

    private void docScan() {
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
            if (mDocScanner == null) {
                VisionLog.info(TAG, "mDocScanner is null");
                return;
            }
            VisionResult<DocScanResults> visionResult = mDocScanner.docScan(visionImage, 0, 1234);
            showResult(visionResult, SystemClock.elapsedRealtime() - startTime);
            if (visionResult.getResultCode() == 0 && visionResult.getResult() != null) {
                mResult = visionResult.getResult();
            } else {
                mResult = null;
            }
            close();
        });
    }

    protected void showToast(String msg) {
        if (TextUtils.isEmpty(msg)) {
            return;
        }
        runOnUiThread(() -> Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }

    private void docWarp() {
        if (mResult == null) {
            showToast("请先进行文档扫描");
            VisionLog.debug(TAG, "mResult is null");
            return;
        }
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
            if (mDocScanner == null) {
                VisionLog.info(TAG, "mDocScanner is null");
                return;
            }
            VisionResult<DocScanImage> visionResult = mDocScanner.docWarp(visionImage, initDocWrap());
            showResult(visionResult, SystemClock.elapsedRealtime() - startTime);
            if (visionResult.getResultCode() == 0 && visionResult.getResult() != null) {
                mDocScanImage = visionResult.getResult();
                runOnUiThread(() -> mBinding.resultImage.setImageBitmap(mDocScanImage.getBitmap()));
            } else {
                mDocScanImage = null;
            }
            close();
        });
    }

    private DocWarpPara initDocWrap() {
        DocWarpPara docWarpPara = new DocWarpPara();
        docWarpPara.setDocScanCoords(DocScanCoords.toFloatArray(mResult.getDocScanCoords()));
        docWarpPara.setIsRemoveShadow(true);
        docWarpPara.setRatioX(1.0f);
        docWarpPara.setRatioY(1.0f);
        return docWarpPara;
    }

    private void docFilter() {
        if (bitmap == null) {
            VisionLog.debug(TAG, "bitmap is null");
            return;
        }
        String num = mBinding.edFilter.getText().toString();
        if (TextUtils.isEmpty(num)) {
            showToast("请先设置参数");
            return;
        }
        VisionImage visionImage = VisionImage.fromBitmap(bitmap);
        mTime = SystemClock.elapsedRealtime();
        long startTime = SystemClock.elapsedRealtime();
        getClient();
        open();
        mExecutorService.submit(() -> {
            if (mDocScanner == null) {
                VisionLog.info(TAG, "mDocScanner is null");
                return;
            }
            VisionResult<DocScanImage> visionResult =
                mDocScanner.docFilter(visionImage, DocScanFilter.BLACK_AND_WHITE);
            showResult(visionResult, SystemClock.elapsedRealtime() - startTime);
            if (visionResult.getResultCode() == 0 && visionResult.getResult() != null) {
                runOnUiThread(() -> mBinding.resultImage.setImageBitmap(visionResult.getResult().getBitmap()));
            }
            close();
        });
    }

    private void docEnd() {
        if (mResult == null) {
            showToast("请先进行文档扫描");
            VisionLog.debug(TAG, "mResult is null");
            return;
        }
        mTime = SystemClock.elapsedRealtime();
        long startTime = SystemClock.elapsedRealtime();
        getClient();
        open();
        mExecutorService.submit(() -> {
            if (mDocScanner == null) {
                VisionLog.info(TAG, "mDocScanner is null");
                return;
            }
            VisionResult<Integer> visionResult = mDocScanner.docScanEnd(1234);
            showResult(visionResult, SystemClock.elapsedRealtime() - startTime);
            close();
        });
    }

    private void close() {
        mExecutorService.submit(() -> {
            if (mDocScanner != null) {
                mDocScanner.close();
                mDocScanner = null;
            }
        });
    }

    private void resetStatus() {
        mBinding.matchResultTextView.setText("");
    }

    @Override
    protected void onDestroy() {
        VisionLog.info(TAG, "onDestroy");
        mExecutorService.execute(this::close);
        super.onDestroy();
    }

    @SuppressLint("RestrictedApi")
    private void showResult(VisionResult<?> result, long costTime) {
        mExecutorService.submit(() -> runOnUiThread(() -> {
            Object result1 = result.getResult();
            Gson gson = new GsonBuilder().create();
            String resultJson;
            if (result1 instanceof DocScanResults) {
                DocScanResults docScanResults = (DocScanResults) result1;
                resultJson = gson.toJson(docScanResults);
            } else if (result1 instanceof DocScanImage) {
                DocScanImage docScanImage = (DocScanImage) result1;
                resultJson = gson.toJson(docScanImage);
            } else if (result1 instanceof Integer) {
                Integer integer = (Integer) result1;
                resultJson = String.valueOf(integer);
            } else {
                resultJson = "not match any";
            }
            mBinding.matchResultTextView.append("模型运行时延：" + costTime + "ms, resultCode = " + result.getResultCode()
                + ", resultResult = " + resultJson + "\n");
        }));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mBinding.matchPreviewPicImageView.setVisibility(View.VISIBLE);
        bitmap = Utils.onActivityResult(requestCode, resultCode, data, this);
        mBinding.matchPreviewPicImageView.setImageBitmap(bitmap);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
        @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Utils.onRequestPermissionsResult(requestCode, this, grantResults);
    }
}