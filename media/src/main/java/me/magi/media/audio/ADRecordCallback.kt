package me.magi.media.audio

internal interface ADRecordCallback {

    fun onRecordStart()

    fun onRecordPcmData(data: ByteArray, length: Int, timeMills: Long)

    fun onRecordStop()

    fun onRecordError(errorCode:Int, errorMsg: String)

    fun onRecordFinish()

}