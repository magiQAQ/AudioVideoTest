package me.magi.media.video

import android.util.Size

class ADPushConfig {

    internal var mSize = Size(1280, 720)
    internal var mCameraFacing = ADCameraConstant.CAMERA_FACING_BACK
    internal var mCameraIndex = 0

    /**
     * 获取前置摄像头数量
      */
    fun getFrontCameraCount(): Int {
        return ADCameraController.getFrontCameraCount()
    }

    /**
     * 获取后置摄像头数量
     */
    fun getBackCameraCount(): Int {
        return ADCameraController.getBackCameraCount()
    }

    /**
     * 设置拍摄的分辨率
     */
    fun setResolution(@ADCameraConstant.ADResolutionDef resolution: Int) {
        mSize = when (resolution) {
            ADCameraConstant.RESOLUTION_640_360 -> Size(640, 360)
            ADCameraConstant.RESOLUTION_1280_720 -> Size(1280, 720)
            ADCameraConstant.RESOLUTION_1920_1080 -> Size(1920, 1080)
            else -> Size(1280, 720)
        }
    }

    /**
     * 设置默认打开的摄像头方向（打开前置还是后置）
     */
    fun setCameraFacing(@ADCameraConstant.ADFacingDef cameraFacing: Int) {
        mCameraFacing = cameraFacing
    }

    fun setCameraIndex(cameraIndex: Int) {

    }
}