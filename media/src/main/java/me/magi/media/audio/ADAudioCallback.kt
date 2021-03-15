package me.magi.media.audio

import java.io.File

interface ADAudioCallback {
    fun onError(errorCode: Int, errorMsg: String)
    fun onSaveFinish(wavFile: File)
    fun onPlayTime(currentTime: Int, totalTime: Int)
}