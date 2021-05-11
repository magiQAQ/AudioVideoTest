package me.magi.media.video

import android.util.Size

class ADPushConfig {

    private var size = Size(1280, 720)

    fun setResolution(@ADCameraConstant.ADResolutionDef resolution: Int) {
        size = when (resolution) {
            ADCameraConstant.RESOLUTION_640_360 -> Size(640, 360)
            ADCameraConstant.RESOLUTION_1280_720 -> Size(1280, 720)
            ADCameraConstant.RESOLUTION_1920_1080 -> Size(1920, 1080)
            else -> Size(1280, 720)
        }
    }

    internal fun getWidth(): Int = size.width
    internal fun getHeight(): Int = size.height
}