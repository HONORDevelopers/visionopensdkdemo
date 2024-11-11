/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

package com.hihonor.vision.demo.codescan;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.hihonor.mcs.intelligence.vision.codescan.utils.CodeScanUtil;
import com.hihonor.mcs.intelligence.visionkit.data.codescan.CodeScanConstant;
import com.hihonor.mcs.intelligence.visionkit.data.codescan.ScanResult;
import com.hihonor.mcs.intelligence.visionkit.log.VisionLog;
import com.hihonor.visiondemo.R;

import java.util.ArrayList;

/**
 * 预览流执行码检测,测试demo
 *
 * @author ww0067677
 * @since 2024-10-23
 */
@SuppressLint("RestrictedApi")
public class PreCodeScanDetectActivity extends AppCompatActivity implements View.OnClickListener {
    private static final String TAG = "PreCodeScanDetectActivity";

    public static final String ANDROID_PERMISSION_CAMERA = "android.permission.CAMERA";

    public static final String[] PERMISSIONS_CAMERA = {ANDROID_PERMISSION_CAMERA};

    private Button mBtnFirst;

    private Button mBtnSecond;

    private TextView mResultView;

    private final ActivityResultLauncher<Intent> mStartForResult = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                showScanResult(result.getResultCode(), result.getData());
            }
        });

    private void showScanResult(int resultCode, Intent data) {
        if (resultCode == RESULT_OK && data != null) {
            ScanResult scanResult = data.getParcelableExtra(CodeScanConstant.PARCELABLE_SCAN_RESULT);
            String result = scanResult == null ? "" : scanResult.toString();
            VisionLog.info(TAG, "onActivityResult scanResult=" + result);
            mResultView.setText(result);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        setContentView(R.layout.activity_pre_code_scan);

        mResultView = findViewById(R.id.match_result_text_view); // 显示结果
        mResultView.setMovementMethod(ScrollingMovementMethod.getInstance()); // 设置为滚动

        mBtnFirst = findViewById(R.id.btn_pre_code_san_first);// 选图片按钮
        mBtnFirst.setOnClickListener(this);

        mBtnSecond = findViewById(R.id.btn_pre_code_san_second);// 码解析
        mBtnSecond.setOnClickListener(this);

    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        resetStatus();
        testApi(id);
    }

    private void testApi(int id) {
        VisionLog.info(TAG, "api");
        switch (id) {
            case R.id.btn_pre_code_san_first:
                if (ActivityCompat.checkSelfPermission(this, ANDROID_PERMISSION_CAMERA) == 0) {
                    CodeScanUtil.startScan(this, CodeScanConstant.REQUEST_ACTIVITY_RESULT, getBarcodeFormat());
                } else {
                    mResultView.setText("Plugin Connect error or app did not have code_scan ability.");
                    ActivityCompat.requestPermissions(this, PERMISSIONS_CAMERA, CodeScanConstant.REQUEST_PERMISSIONS);
                }
                break;
            case R.id.btn_pre_code_san_second:
                if (ActivityCompat.checkSelfPermission(this, ANDROID_PERMISSION_CAMERA) == 0) {
                    CodeScanUtil.startScan(this, this.mStartForResult, getBarcodeFormat());
                } else {
                    ActivityCompat.requestPermissions(this, PERMISSIONS_CAMERA, CodeScanConstant.REQUEST_PERMISSIONS);
                }
                break;
            default:
                VisionLog.error(TAG, "do nothing");
        }
    }

    private ArrayList<String> getBarcodeFormat() {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(CodeScanConstant.FORMAT_QR_CODE);
        return arrayList;
    }

    private void resetStatus() {
        mResultView.setText("");
    }

    @Override
    protected void onDestroy() {
        VisionLog.info(TAG, "onDestroy");
        super.onDestroy();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
        @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CodeScanConstant.REQUEST_PERMISSIONS && permissions.length > 0) {
            CodeScanUtil.startScan(this, CodeScanConstant.REQUEST_ACTIVITY_RESULT, getBarcodeFormat());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CodeScanConstant.REQUEST_ACTIVITY_RESULT) {
            showScanResult(resultCode, data);
        }
    }
}