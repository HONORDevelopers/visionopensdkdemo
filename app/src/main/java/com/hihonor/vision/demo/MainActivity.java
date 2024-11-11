/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

package com.hihonor.vision.demo;

import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.hihonor.vision.demo.codescan.CodeScanDetectActivity;
import com.hihonor.vision.demo.codescan.PreCodeScanDetectActivity;
import com.hihonor.vision.demo.docscan.DocScanActivity;
import com.hihonor.vision.demo.focusocr.FocusOcrActivity;
import com.hihonor.visiondemo.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页
 *
 * @author w0011281
 * @since 2024-03-11
 */
public class MainActivity extends AppCompatActivity {
    private static List<Pair<String, Class>> functionList = new ArrayList<>();

    private ListView mLvFunctions;

    static {
        functionList.add(new Pair<>("文档扫描", DocScanActivity.class));
        functionList.add(new Pair<>("文字识别算法", FocusOcrActivity.class));
        functionList.add(new Pair<>("码检测（海外扫码、wifi扫码）", CodeScanDetectActivity.class));
        functionList.add(new Pair<>("预览流扫码", PreCodeScanDetectActivity.class));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mLvFunctions = findViewById(R.id.lv_functions);// todo 改为可滑动viewList
        mLvFunctions.setAdapter(new FunctionAdapter());
        mLvFunctions.setOnItemClickListener((parent, view, position, id) -> startActivity(
            new Intent(MainActivity.this, (Class<?>) mLvFunctions.getAdapter().getItem(position))));
    }

    class FunctionAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return functionList.size();
        }

        @Override
        public Object getItem(int position) {
            return functionList.get(position).second;
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView holder = null;
            if (convertView == null) {
                convertView =
                    LayoutInflater.from(getApplicationContext()).inflate(R.layout.item_listview, parent, false); // 加载布局
                holder = convertView.findViewById(R.id.tv_function_name);
                convertView.setTag(holder);
            } else {
                holder = (TextView) convertView.getTag();
            }
            holder.setText(functionList.get(position).first);
            return convertView;
        }
    }
}