package me.magi.media.video

import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.util.Size
import me.magi.media.utils.ADAppUtil

class ADCameraInfo(cameraId: String) {

    private val mCameraCharacteristics = ADAppUtil.cameraManager.getCameraCharacteristics(cameraId)
    private val mKeys = mCameraCharacteristics.keys

    fun getCameraOrientation(): Int {
        return if (mKeys.contains(CameraCharacteristics.SENSOR_ORIENTATION)) {
            mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        } else -1
    }

    fun getOutputSize(): Array<Size>? {
        return if (mKeys.contains(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)) {
            val map = mCameraCharacteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
            map?.getOutputSizes(ImageFormat.YUV_420_888)
        } else null
    }
}