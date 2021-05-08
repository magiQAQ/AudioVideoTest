package me.magi.media.video

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import me.magi.media.utils.ADAppUtil

class ADCameraInfo(cameraId: String) {

    private val mCameraCharacteristics = ADAppUtil.cameraManager.getCameraCharacteristics(cameraId)

    fun getCameraOrientation(): Int {
        return mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
    }

    fun getOutputSize(): Array<Size>? {
        return mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)!!.getOutputSizes(ImageFormat.YUV_420_888)
    }

    fun isSupportFlash(): Boolean {
        return mCameraCharacteristics.get(CameraCharacteristics.FLASH_INFO_AVAILABLE)?:false
    }
}