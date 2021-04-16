package me.magi.media.video

import android.hardware.camera2.CameraCharacteristics
import me.magi.media.utils.ADAppUtil

class ADCameraInfo(private val cameraId: String) {

    private val mCameraCharacteristics = ADAppUtil.cameraManager.getCameraCharacteristics(cameraId)
    private val mKeys = mCameraCharacteristics.keys

    fun getCameraOrientation(): Int {
        return if (mKeys.contains(CameraCharacteristics.SENSOR_ORIENTATION)) {
            mCameraCharacteristics.get(CameraCharacteristics.SENSOR_ORIENTATION)!!
        } else -1
    }


}