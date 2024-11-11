/*
 * Copyright (c) Honor Device Co., Ltd. 2024-2024. All rights reserved.
 */

package com.hihonor.vision.demo.utils;

import android.content.Context;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DecodeFormat;
import com.bumptech.glide.request.RequestOptions;
import com.hihonor.visiondemo.R;
import com.lcw.library.imagepicker.utils.ImageLoader;

/**
 * 实现自定义图片加载
 *
 * @author ww0067677
 * @since 2024-10-23
 */
public class GlideLoader implements ImageLoader {
    private Context mContext;

    public GlideLoader(Context context) {
        this.mContext = context;
    }

    private RequestOptions mOptions = new RequestOptions().centerCrop()
        .format(DecodeFormat.PREFER_RGB_565)
        .placeholder(R.mipmap.ic_launcher_round)
        .error(R.mipmap.ic_launcher_round);

    private RequestOptions mPreOptions = new RequestOptions().skipMemoryCache(true).error(R.mipmap.ic_launcher_round);

    @Override
    public void loadImage(ImageView imageView, String imagePath) {
        // 小图加载
        Glide.with(imageView.getContext()).load(imagePath).apply(mOptions).into(imageView);
    }

    @Override
    public void loadPreImage(ImageView imageView, String imagePath) {
        // 大图加载
        Glide.with(imageView.getContext()).load(imagePath).apply(mPreOptions).into(imageView);

    }

    @Override
    public void clearMemoryCache() {
        // 清理缓存
        Glide.get(mContext.getApplicationContext()).clearMemory();
    }
}
