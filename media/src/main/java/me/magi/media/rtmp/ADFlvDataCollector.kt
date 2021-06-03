package me.magi.media.rtmp

internal interface ADFlvDataCollector {
    fun collect(flvData: ADFlvData, type: Int)
}