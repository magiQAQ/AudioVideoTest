package me.magi.audioVideoTest.audio

import java.io.File

interface ADAudioCallback {
    fun onError(errorCode: Int, errorMsg: String)
    fun onSaveFinish(wavFile: File)
}