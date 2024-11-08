
package com.hihonor.vision.demo;

import android.app.Application;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CameraXConfig;

/**
 * 功能描述
 *
 * @since 2024-08-08
 */
public class VisionDemoApplication extends Application implements CameraXConfig.Provider {
    @Override
    public CameraXConfig getCameraXConfig() {
        return Camera2Config.defaultConfig();
    }
}
