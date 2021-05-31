package me.magi.media.video

import me.magi.media.utils.ADLiveConstant

class ADVideoConfig {
    var targetWidth = 1280
    var targetHeight = 720
    var previewWidth = 1280
    var previewHeight = 720
    var targetFps = 30
    var previewMaxFPS = 30
    var previewMinFPS = 30
    var screenOrientation = ADLiveConstant.ORIENTATION_PORTRAIT
    var cameraFacing = ADLiveConstant.CAMERA_FACING_BACK
    var cameraIndex = 0

    var cropRatio = 1f

}