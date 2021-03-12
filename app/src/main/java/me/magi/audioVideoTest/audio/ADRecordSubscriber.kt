package me.magi.audioVideoTest.audio

class ADRecordSubscriber {
    var recordStart: (() -> Unit)? = null
    var recordStop: (() -> Unit)? = null
    var recordError: ((errorCode:Int, errorMsg: String) -> Unit)? = null
    var recordPcmData: ((data: ByteArray, length: Int, timeMills: Long) -> Unit)? = null

    fun onRecordStart(recordStart: () -> Unit) {
        this.recordStart = recordStart
    }

    fun onRecordStop(recordStop: () -> Unit) {
        this.recordStop = recordStop
    }

    fun onRecordError(recordError: (errorCode:Int, errorMsg: String) -> Unit) {
        this.recordError = recordError
    }

    fun onRecordPcmData(recordPcmData: (data: ByteArray, length: Int, timeMills: Long) -> Unit) {
        this.recordPcmData = recordPcmData
    }

}