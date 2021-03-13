package me.magi.audioVideoTest.audio

import java.io.File

interface ADAudioRecordCallback {
    fun onError(errorCode: Int, errorMsg: String)
    fun onRecordFinish(wavFile: File)
}