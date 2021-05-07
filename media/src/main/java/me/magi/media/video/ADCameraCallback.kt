package me.magi.media.video

fun interface ADCameraCallback {
    fun onError(errorCode: Int, errorMsg: String)
}