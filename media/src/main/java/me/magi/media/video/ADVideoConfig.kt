package me.magi.media.video

import me.magi.media.utils.ADLiveConstant

class ADVideoConfig {
    // 用户设定的分辨率
    var targetWidth = 1280
    var targetHeight = 720
    // 摄像头实际使用的分辨率
    var previewWidth = 1280
    var previewHeight = 720
    // 渲染时的分辨率
    var videoWidth = 720
    var videoHeight = 1280
    // 用户设定的帧数
    var targetFPS = 30
    // 摄像头实际使用的帧数, 渲染时使用该最大帧数
    var previewMaxFPS = 30
    var previewMinFPS = 30
    // 用户设置的屏幕方向
    var screenOrientation = ADLiveConstant.ORIENTATION_PORTRAIT
    // 用户设置的摄像头
    var cameraFacing = ADLiveConstant.CAMERA_FACING_BACK
    var cameraIndex = 0
    // 根据摄像头的分辨率和渲染的分辨率,进行裁剪的比例
    var cropRatio = 0f

}