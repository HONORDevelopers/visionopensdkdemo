/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

package com.hihonor.vision.demo.codescan;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.ImageFormat;
import android.os.Bundle;
import android.os.SystemClock;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.hihonor.mcs.intelligence.vision.codescan.VisionCodeScanner;
import com.hihonor.mcs.intelligence.vision.common.exception.VisionResultException;
import com.hihonor.mcs.intelligence.visionkit.core.FeatureAbility;
import com.hihonor.mcs.intelligence.visionkit.data.BundleKey;
import com.hihonor.mcs.intelligence.visionkit.data.VisionImage;
import com.hihonor.mcs.intelligence.visionkit.data.VisionImageMetadata;
import com.hihonor.mcs.intelligence.visionkit.data.VisionResult;
import com.hihonor.mcs.intelligence.visionkit.data.codescan.CodeScanConstant;
import com.hihonor.mcs.intelligence.visionkit.data.codescan.ScanResult;
import com.hihonor.mcs.intelligence.visionkit.log.VisionLog;
import com.hihonor.vision.demo.utils.ImageUtil;
import com.hihonor.vision.demo.utils.Utils;
import com.hihonor.visiondemo.R;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 码检测,测试demo
 *
 * @author ww0067677
 * @since 2024-10-23
 */
@SuppressLint("RestrictedApi")
public class CodeScanDetectActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "CodeScanDetectActivity";

    private ExecutorService mExecutorService = Executors.newSingleThreadExecutor();

    private Button mBtnPickUp;

    private ImageView mPreviewPhoto;

    private TextView mResultView;

    private Button mBtnImage;

    private Button mBtnConnService;

    private Button mBtnDisConnService;

    private long mTime;

    private VisionCodeScanner mCodeScanDetect;

    private int keepAliveTime = 10 * 60;

    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        setContentView(R.layout.activity_code_scan);

        mResultView = findViewById(R.id.match_result_text_view); // 显示结果
        mResultView.setMovementMethod(ScrollingMovementMethod.getInstance()); // 设置为滚动

        mBtnPickUp = findViewById(R.id.match_select_photo_btn);// 选图片按钮
        mBtnPickUp.setOnClickListener(this);

        mPreviewPhoto = findViewById(R.id.match_preview_pic_image_view);// 显示图片

        mBtnImage = findViewById(R.id.detect_code_btn);// 码解析
        mBtnImage.setOnClickListener(this);

        mBtnConnService = findViewById(R.id.conn_service_btn);
        mBtnConnService.setOnClickListener(this);

        mBtnDisConnService = findViewById(R.id.dis_conn_service_btn);
        mBtnDisConnService.setOnClickListener(this);

        mExecutorService.execute(() -> bitmap = ImageUtil.getBitmap(this, R.drawable.default_face));
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        resetStatus();
        mExecutorService.execute(() -> testApi(id));
    }

    private void testApi(int id) {
        VisionLog.info(TAG, "api");
        mTime = SystemClock.elapsedRealtime();
        switch (id) {
            case R.id.match_select_photo_btn:// 选择图片
                Utils.pickUpPhoto(this);
                break;
            case R.id.detect_code_btn:// 检测码
                getClient();
                open();
                codeScan();
                break;
            case R.id.conn_service_btn:// 测试连接服务以及插件加载
                getClient();
                open();
                break;
            case R.id.dis_conn_service_btn:
                close();
                break;
            default:
                VisionLog.error(TAG, "do nothing");
        }
    }

    private void getClient() {
        if (mCodeScanDetect == null) {
            ArrayList<String> barcodeFormat = new ArrayList<>();
            barcodeFormat.add(CodeScanConstant.FORMAT_QR_CODE); // 在这里只有QR_CODE这一种格式
            HashMap<Object, Object> extraInfo = new HashMap<>();
            /** 不传加载全部支持的格式,根据实际情况选择是否添加 */
            extraInfo.put(BundleKey.BAR_CODE_FORMAT, barcodeFormat);
            extraInfo.put(BundleKey.IS_OPEN_SOURCE_CODE_SCAN, true);// true是默认值不用设置
            // keepAliveTime 保活时间单位秒，businessName 应用名_插件名_使用场景
            try {
                mCodeScanDetect = VisionCodeScanner.getClient(getApplicationContext(), keepAliveTime,
                    "honorlens_codescan_wisdom", extraInfo);
                FeatureAbility availability = mCodeScanDetect.getAvailability(getApplicationContext());
                runOnUiThread(() -> mResultView.setText("当前插件状态:  " + availability + "\n"));
            } catch (VisionResultException e) {
                VisionLog.error(TAG, "VisionResultException", e);
            }
        }
    }

    private void open() {
        if (mCodeScanDetect == null) {
            VisionLog.info(TAG, "mCodeScanDetect is null");
            return;
        }
        mTime = SystemClock.elapsedRealtime();
        int code = mCodeScanDetect.open();
        VisionLog.info(TAG, "open code = " + code);
        String spendTime = (SystemClock.elapsedRealtime() - mTime) + "ms";
        mExecutorService.submit(() -> runOnUiThread(() -> {
            Toast.makeText(CodeScanDetectActivity.this, "插件加载成功！", Toast.LENGTH_LONG).show();
            mResultView.append("模型加载时延：" + spendTime + ", resultCode = " + code + "\n");
        }));
    }

    private void codeScan() {
        if (mCodeScanDetect == null) {
            VisionLog.info(TAG, "mCodeScanDetect is null");
            return;
        }
        if (bitmap == null) {
            VisionLog.debug(TAG, "bitmap is null");
            return;
        }
        VisionImage visionImage = VisionImage.fromBitmap(bitmap);
        VisionImageMetadata inputImageMetadata = new VisionImageMetadata.Builder().setWidth(bitmap.getWidth())
            .setHeight(bitmap.getHeight())
            .setFormat(ImageFormat.YUV_420_888)
            .build();
        visionImage.setMetadata(inputImageMetadata);
        mTime = SystemClock.elapsedRealtime();
        long startTime = SystemClock.elapsedRealtime();
        VisionResult<ArrayList<ScanResult>> arrayListVisionResult = mCodeScanDetect.codeScan(visionImage, 1, 1);
        showResult(arrayListVisionResult, SystemClock.elapsedRealtime() - startTime);
    }

    private void close() {
        if (mCodeScanDetect != null) {
            mCodeScanDetect.close();
            mCodeScanDetect = null;
        }
    }

    private void resetStatus() {
        mResultView.setText("");
    }

    @Override
    protected void onDestroy() {
        VisionLog.info(TAG, "onDestroy");
        mExecutorService.execute(() -> close());
        super.onDestroy();
    }

    @SuppressLint("RestrictedApi")
    private void showResult(VisionResult vectorInfo, long costTime) {
        if (vectorInfo == null || vectorInfo.getResult() == null || vectorInfo.getResultCode() != 0) {
            VisionLog.debug(TAG, "result is null");
            return;
        }

        StringBuffer stringBuffer = new StringBuffer();
        if (vectorInfo.getResult() instanceof ArrayList) {
            ArrayList<ScanResult> codeScanResults = (ArrayList<ScanResult>) vectorInfo.getResult();
            for (ScanResult scanResult : codeScanResults) {
                stringBuffer.append(scanResult.toString() + "\n\n\n");
            }
        }
        mExecutorService.submit(() -> runOnUiThread(() -> mResultView.append("模型运行时延：" + costTime + "ms, resultCode = "
            + vectorInfo.getResultCode() + ", resultResult =" + stringBuffer + "\n")));
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mPreviewPhoto.setVisibility(View.VISIBLE);
        bitmap = Utils.onActivityResult(requestCode, resultCode, data, this);
        mPreviewPhoto.setImageBitmap(bitmap);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
        @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        Utils.onRequestPermissionsResult(requestCode, this, grantResults);
    }
}