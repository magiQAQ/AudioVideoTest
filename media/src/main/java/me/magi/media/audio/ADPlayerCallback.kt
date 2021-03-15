package me.magi.media.audio

interface ADPlayerCallback {

    fun onPlayStart()

    fun onPlayTime(currentTime: Int, totalTime: Int)

    fun onPlayEnd()

    fun onPlayError(errorCode: Int, errorMsg: String)

    fun onPlayFinish()

}