package me.magi.media.video

import android.graphics.ImageFormat
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.util.Range
import android.util.Size
import me.magi.media.utils.ADAppUtil

class ADCameraInfo(cameraId: String) {

    private val mCameraCharacteristics = ADAppUtil.cameraManager.getCameraCharacteristics(cameraId)

    fun getCameraOrientation(): Int {
        return mCameraCharacteristics[CameraCharacteristics.SENSOR_ORIENTATION]!!
    }

    fun getOutputSize(): Array<Size>? {
        return mCameraCharacteristics[CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP]!!.getOutputSizes(SurfaceTexture::class.java)
    }

    fun isSupportFlash(): Boolean {
        return mCameraCharacteristics[CameraCharacteristics.FLASH_INFO_AVAILABLE]!!
    }

    fun getCameraFacing(): Int {
        return mCameraCharacteristics[CameraCharacteristics.LENS_FACING]!!
    }

    fun getFpsRanges(): Array<Range<Int>> {
        return mCameraCharacteristics[CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES]!!
    }
}