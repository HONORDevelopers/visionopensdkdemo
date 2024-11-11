
package com.hihonor.vision.demo.utils;

import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.os.Build;
import android.os.Handler;
import android.os.SystemClock;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicYuvToRGB;
import android.renderscript.Type;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.hihonor.mcs.intelligence.visionkit.data.VisionImage;
import com.hihonor.mcs.intelligence.visionkit.data.VisionImageMetadata;
import com.hihonor.mcs.intelligence.visionkit.log.VisionLog;
import com.hihonor.mcs.intelligence.visionkit.utils.BitmapUtils;
import com.hihonor.mcs.intelligence.visionkit.utils.VisionImageFormat;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Locale;
import java.util.Optional;

/**
 * Function description:
 *
 * @author w0011281
 * @since 2021/3/22
 */
public class ImageUtil {
    private static final String TAG = "ImageUtil";

    /**
     * 图片放大
     *
     * @param context context
     * @param drawable 图片drawable
     */
    public static void onThumbnailClick(Context context, Drawable drawable) {
        final Dialog dialog = new Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        ImageView imgView = getView(context, drawable);
        dialog.setContentView(imgView);
        dialog.show();

        imgView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }

    private static ImageView getView(Context context, Drawable drawable) {
        ImageView imgView = new ImageView(context);
        imgView.setLayoutParams(new RelativeLayout.LayoutParams(RelativeLayout.LayoutParams.MATCH_PARENT,
            RelativeLayout.LayoutParams.MATCH_PARENT));
        imgView.setImageDrawable(drawable);

        return imgView;
    }

    /**
     * 获取文件夹里的图片路径
     *
     * @param context context
     * @param picFolder 文件夹路径
     * @param hasVideo 是否包含视频
     * @return 图片路径
     */
    public static ArrayList<String> getFilesAllName(Context context, File picFolder, boolean hasVideo) {
        ArrayList<String> imagePaths = new ArrayList<>();

        if (!picFolder.exists()) {
            picFolder.mkdir();
            boolean isFilemaked1 = picFolder.isDirectory();
            boolean isFilemaked2 = picFolder.mkdirs();

            if (isFilemaked1 || isFilemaked2) {
                VisionLog.info(TAG, "创建文件夹成功");
                imagePaths = getAllImages(context, picFolder.getPath(), hasVideo);
            } else {
                VisionLog.info(TAG, "创建文件夹失败");
            }
        } else {
            VisionLog.info(TAG, "文件夹已存在");
            imagePaths = getAllImages(context, picFolder.getPath(), hasVideo);
        }
        return imagePaths;
    }

    private static ArrayList<String> getAllImages(Context context, String path, boolean hasVideo) {
        File file = new File(path);
        ArrayList<String> imagePaths = new ArrayList<>();
        File[] files = file.listFiles();
        if (files != null && files.length > 0) {
            for (int i = 0; i < files.length; i++) {
                if (checkIsImageFile(files[i].getPath())) {
                    imagePaths.add(files[i].getPath());
                }
                if (hasVideo && checkIsVideoFile(files[i].getPath())) {
                    imagePaths.add(files[i].getPath());
                }
            }
        } else {
            Toast.makeText(context, "没有图片", Toast.LENGTH_LONG).show();
        }
        return imagePaths;
    }

    /**
     * 判断是否是照片
     *
     * @param fName 文件路径
     * @return true:是图片
     */
    private static boolean checkIsImageFile(String fName) {
        boolean isImageFile = false;
        // 获取拓展名
        String fileEnd = fName.substring(fName.lastIndexOf(".") + 1, fName.length()).toLowerCase();
        if ("jpg".equals(fileEnd) || "png".equals(fileEnd) || "jpeg".equals(fileEnd) || "bmp".equals(fileEnd)
            || "gif".equals(fileEnd) || "heif".equals(fileEnd)) {
            isImageFile = true;
        } else {
            isImageFile = false;
        }
        return isImageFile;
    }

    /**
     * 判断是否是视频
     *
     * @param fName 文件路径
     * @return true:是视频
     */
    public static boolean checkIsVideoFile(String fName) {
        boolean isVideoFile = false;
        // 获取拓展名
        String fileEnd = fName.substring(fName.lastIndexOf(".") + 1, fName.length()).toLowerCase();
        if ("mp4".equals(fileEnd) || "flv".equals(fileEnd)) {
            isVideoFile = true;
        } else {
            isVideoFile = false;
        }
        return isVideoFile;
    }

    /**
     * 保存图片
     *
     * @param bmpFolder 文件路径
     * @param fileName 文件名
     * @param bitmap 图片
     */
    public static void saveBitmap(File bmpFolder, String fileName, Bitmap bitmap) {
        boolean isFolderExists = false;
        if (!bmpFolder.exists()) {
            bmpFolder.mkdir();
            boolean isFilemaked1 = bmpFolder.isDirectory();
            boolean isFilemaked2 = bmpFolder.mkdirs();

            if (isFilemaked1 || isFilemaked2) {
                VisionLog.info(TAG, "创建文件夹成功");
                isFolderExists = true;
            } else {
                VisionLog.info(TAG, "创建文件夹失败");
                isFolderExists = false;
            }
        } else {
            VisionLog.info(TAG, "文件夹已存在");
            isFolderExists = true;
        }

        if (isFolderExists) {
            VisionLog.info(TAG, "fileName:" + fileName);
            File file = new File(bmpFolder, fileName);
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
                if (bitmap != null) {
                    FileOutputStream out = new FileOutputStream(file);
                    bitmap.compress(Bitmap.CompressFormat.PNG, 90, out);
                    out.flush();
                    out.close();
                    VisionLog.info(TAG, "图片保存成功");
                }
            } catch (IOException e) {
                VisionLog.error(TAG, "Exception happened:" + e.getMessage());
            }
        }
    }

    /**
     * 资源文件转bitmap
     *
     * @param context context
     * @param vectorDrawableId 资源文件id
     * @return bitmap
     */
    public static Bitmap getBitmap(Context context, int vectorDrawableId) {
        Bitmap bitmap;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            Drawable vectorDrawable = context.getDrawable(vectorDrawableId);
            bitmap = Bitmap.createBitmap(vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight(),
                Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            vectorDrawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            vectorDrawable.draw(canvas);
        } else {
            bitmap = BitmapFactory.decodeResource(context.getResources(), vectorDrawableId);
        }
        return bitmap;
    }

    /**
     * 图库后台的压缩
     *
     * @param bitmap 要压缩的图片bitmap
     * @return 返回压缩后的图片
     */
    public static Bitmap getCompressImage(Bitmap bitmap) {
        long time = SystemClock.elapsedRealtime();
        int minSize = 224;
        int scaleWidth = bitmap.getWidth() / minSize;
        int scaleHeight = bitmap.getHeight() / minSize;
        int maxScale = Math.min(scaleWidth, scaleHeight);
        int targetWidth = bitmap.getWidth() / maxScale;
        int targetHeight = bitmap.getHeight() / maxScale;
        Bitmap scaleBitmap = Bitmap.createScaledBitmap(bitmap, minSize, minSize, true);
        Log.i(TAG, "scaleBitmap: " + scaleBitmap.getWidth() + "x" + scaleBitmap.getHeight() + " maxScale:" + maxScale
            + "  spendTime: " + (SystemClock.elapsedRealtime() - time));
        return scaleBitmap;
    }

    /**
     * 压缩图片
     *
     * @param path 保存路径
     * @return 返回压缩后的图片
     */
    public static VisionImage getCompressImage(String path) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        File mCacheDir = new File(path);

        File tempFile = new File(path);
        String tempFileName = String.valueOf(path.hashCode()) + ".jpg";
        File cacheFile = new File(mCacheDir, tempFileName);
        VisionLog.info(TAG, "exists:" + cacheFile.exists());

        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path);
            FileDescriptor fd = fis.getFD();
            Optional<Bitmap> bmp = DecodeUtils.getBitmapByMinSize(fd, options, 1080, 2560, 0.25f);
            Bitmap bitmap = DecodeUtils.getBitmapByMaxSize(bmp.get(), tempFileName, mCacheDir, new Handler()).get();
            return VisionImage.fromBitmap(bitmap);
        } catch (FileNotFoundException e) {
        } catch (IOException e) {
        } finally {
            closeSilently(fis);
        }
        return null;
    }

    /**
     * 读取照片exif信息中的旋转角度
     *
     * @param path 照片路径
     * @return 获取从相册中选中图片的角度
     */
    public static int readPictureDegree(String path) {
        if (TextUtils.isEmpty(path)) {
            return 0;
        }
        int degree = 0;
        try {
            ExifInterface exifInterface = new ExifInterface(path);
            int orientation =
                exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch (Exception e) {
        }
        return degree;
    }

    /**
     * 将图片按照某个角度进行旋转
     *
     * @param bm 需要旋转的图片
     * @param degree 旋转角度
     * @return 旋转后的图片
     */
    public static Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
        Bitmap returnBm = null;

        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }

    public static Bitmap intArrayToBitmap(int[] intBitmapArray, int width, int height) {
        // 创建一个空的bitmap
        Bitmap newBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        // 将native函数返回的图片数据设置到空的bitmap中
        newBitmap.setPixels(intBitmapArray, 0, width, 0, 0, width, height);
        return newBitmap;
    }

    public static Bitmap byteArrayToBitmap(byte[] bytes, int width, int height) {
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public static byte[] I420ToNV21(byte[] input, int width, int height) {
        byte[] output = new byte[Integer.MAX_VALUE];
        int frameSize = width * height;
        int qFrameSize = frameSize / 4;
        int tempFrameSize = frameSize * 5 / 4;
        System.arraycopy(input, 0, output, 0, frameSize);

        for (int i = 0; i < qFrameSize; i++) {
            output[frameSize + i * 2] = input[tempFrameSize + i];
            output[frameSize + i * 2 + 1] = input[frameSize + i];
        }
        return output;
    }

    public static Bitmap NV21ToBitmap(Context context, byte[] nv21, int width, int height) {
        RenderScript rs = RenderScript.create(context);
        ScriptIntrinsicYuvToRGB yuvToRgbIntrinsic = ScriptIntrinsicYuvToRGB.create(rs, Element.U8_4(rs));
        Type.Builder yuvType = null;
        yuvType = (new Type.Builder(rs, Element.U8(rs))).setX(nv21.length);
        Allocation in = Allocation.createTyped(rs, yuvType.create(), 1);
        Type.Builder rgbaType = (new Type.Builder(rs, Element.RGBA_8888(rs))).setX(width).setY(height);
        Allocation out = Allocation.createTyped(rs, rgbaType.create(), 1);
        in.copyFrom(nv21);
        yuvToRgbIntrinsic.setInput(in);
        yuvToRgbIntrinsic.forEach(out);
        Bitmap bmpout = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        out.copyTo(bmpout);

        return bmpout;
    }

    /**
     * 将YUV数据转换为Bitmap图片
     *
     * @param frame 源
     * @param width 宽
     * @param height 高
     * @param previewFormat 格式
     * @param quality 图片压缩质量
     * @return 图片
     */
    public static Bitmap yuvToBitmap(byte[] frame, int width, int height, int previewFormat, int quality) {
        YuvImage yuvImage = new YuvImage(frame, previewFormat, width, height, null);
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        yuvImage.compressToJpeg(new Rect(0, 0, width, height), quality, stream);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap bitmap = BitmapFactory.decodeByteArray(stream.toByteArray(), 0, stream.size(), options);
        closeSilently(stream);
        return bitmap;
    }

    /**
     * 关闭流，并且释放与其相关的系统资源
     *
     * @param closeable 可关闭的流对象
     */
    public static void closeSilently(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException t) {
            Log.e(TAG, String.format(Locale.ROOT, "closeSilently got exception: %s", t.getMessage()));
        }
    }

    public static byte[] imageToByteArray(String path) {
        // 将图片文件转化为字节数组字符串
        byte[] data = null;
        // 读取图片字节数组
        try {
            InputStream in = new FileInputStream(path);
            data = new byte[in.available()];
            in.read(data);
            in.close();
        } catch (IOException e) {
            VisionLog.error(TAG, "Exception happened:" + e.getMessage());
        }
        return data;

    }

    /**
     * Bitmap转化为ARGB数据，再转化为NV21数据
     *
     * @param src 传入的Bitmap，格式为Bitmap.Config.ARGB_8888
     * @param width NV21图像的宽度
     * @param height NV21图像的高度
     * @return nv21数据
     */
    public static byte[] bitmapToNv21(Bitmap src, int width, int height) {
        if (src != null && src.getWidth() >= width && src.getHeight() >= height) {
            int[] argb = new int[width * height];
            src.getPixels(argb, 0, width, 0, 0, width, height);
            return argbToNv21(argb, width, height);
        } else {
            return null;
        }
    }

    /**
     * ARGB数据转化为NV21数据
     *
     * @param argb argb数据
     * @param width 宽度
     * @param height 高度
     * @return nv21数据
     */
    private static byte[] argbToNv21(int[] argb, int width, int height) {
        int frameSize = width * height;
        int yIndex = 0;
        int uvIndex = frameSize;
        int index = 0;
        byte[] nv21 = new byte[width * height * 3 / 2];
        for (int j = 0; j < height; ++j) {
            for (int i = 0; i < width; ++i) {
                int rR = (argb[index] & 0xFF0000) >> 16;
                int gG = (argb[index] & 0x00FF00) >> 8;
                int bB = argb[index] & 0x0000FF;
                int yY = (66 * rR + 129 * gG + 25 * bB + 128 >> 8) + 16;
                int uU = (-38 * rR - 74 * gG + 112 * bB + 128 >> 8) + 128;
                int vV = (112 * rR - 94 * gG - 18 * bB + 128 >> 8) + 128;
                nv21[yIndex++] = (byte) (yY < 0 ? 0 : (yY > 255 ? 255 : yY));
                if (j % 2 == 0 && index % 2 == 0 && uvIndex < nv21.length - 2) {
                    nv21[uvIndex++] = (byte) (vV < 0 ? 0 : (vV > 255 ? 255 : vV));
                    nv21[uvIndex++] = (byte) (uU < 0 ? 0 : (uU > 255 ? 255 : uU));
                }
                ++index;
            }
        }
        return nv21;
    }

    /**
     * 将Bimtap转换为NV21的VisionImage
     *
     * @param bitmap 原始Bitmap
     * @param targetWidth 目标Bitmap宽
     * @param targetHeight 目标Bitmap高
     * @param isResizeBitmap 是否resizeBitmap
     * @return VisionImage
     */
    @SuppressLint("RestrictedApi")
    public static VisionImage bitmapToNv21VisionImage(Bitmap bitmap, int targetWidth, int targetHeight,
        boolean isResizeBitmap) {
        Bitmap resizeBitmap = null;
        if (isResizeBitmap && targetWidth > 0 && targetHeight > 0) {
            resizeBitmap = BitmapUtils.resizeByOpencv(bitmap, targetWidth, targetHeight);
        } else {
            resizeBitmap = bitmap;
        }
        byte[] byteArrays = bitmapToNv21(resizeBitmap, resizeBitmap.getWidth(), resizeBitmap.getHeight());
        VisionImageMetadata visionImageMetadata = new VisionImageMetadata.Builder().setWidth(resizeBitmap.getWidth())
            .setHeight(resizeBitmap.getHeight())
            .setFormat(VisionImageFormat.NV21)
            .setRotation(VisionImageMetadata.ROTATION_0)
            .build();
        return VisionImage.fromByteArray(byteArrays, visionImageMetadata);
    }

    /**
     * resizeBitmap将图片转换为目标大小
     *
     * @param inputBitmap Bitmap
     * @param targetWidth int
     * @param targetHeight int
     * @return Bitmap
     */
    public static Bitmap resizeBitmap(Bitmap inputBitmap, int targetWidth, int targetHeight) {
        if (inputBitmap == null) {
            Log.e("BitmapUtils", "inputBitmap is null");
            return null;
        } else {
            Log.i("BitmapUtils", "resizeBitmap started");
            long beginTime = System.currentTimeMillis();
            Bitmap outputBitmap;
            if (inputBitmap.getConfig() == Bitmap.Config.ARGB_8888) {
                outputBitmap = Bitmap.createScaledBitmap(inputBitmap, targetWidth, targetHeight, true);
            } else {
                Bitmap bmp = inputBitmap.copy(Bitmap.Config.ARGB_8888, true);
                outputBitmap = Bitmap.createScaledBitmap(bmp, targetWidth, targetHeight, true);
                bmp.recycle();
            }

            long endTime = System.currentTimeMillis();
            Log.i("BitmapUtils", "resizeBitmap stopped, cost " + (endTime - beginTime) + " ms.");
            return outputBitmap;
        }
    }
}
