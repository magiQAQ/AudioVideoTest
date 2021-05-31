package me.magi.media

import android.util.Size
import me.magi.media.utils.ADLiveConstant
import me.magi.media.video.ADCameraController

class ADPushConfig {

    internal var mSize = Size(1280, 720)
        private set
    internal var mCameraFacing = ADLiveConstant.CAMERA_FACING_BACK
    internal var mCameraIndex = 0
    internal var mScreenOrientation = ADLiveConstant.ORIENTATION_PORTRAIT
    internal var mVideoFPS = 30

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
    fun setResolution(@ADLiveConstant.ADResolutionDef resolution: Int) {
        mSize = when (resolution) {
            ADLiveConstant.RESOLUTION_640_360 -> Size(640, 360)
            ADLiveConstant.RESOLUTION_1280_720 -> Size(1280, 720)
            ADLiveConstant.RESOLUTION_1920_1080 -> Size(1920, 1080)
            else -> Size(1280, 720)
        }
    }

    /**
     * 设置默认打开的摄像头方向（打开前置还是后置）
     */
    fun setCameraFacing(@ADLiveConstant.ADFacingDef cameraFacing: Int) {
        mCameraFacing = cameraFacing
    }

    fun setCameraIndex(cameraIndex: Int) {
        mCameraIndex = cameraIndex
    }

    fun setOrientation(@ADLiveConstant.ADOrientationDef screenOrientation: Int) {
        mScreenOrientation = screenOrientation
    }
}