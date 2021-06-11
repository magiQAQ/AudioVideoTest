package com.magi.adlive.encode

import android.media.MediaCodec
import android.media.MediaFormat

interface EncodeCallback {

    @Throws(IllegalStateException::class)
    fun inputAvailable(mediaCodec: MediaCodec, inBufferIndex: Int)

    @Throws(IllegalStateException::class)
    fun outputAvailable(mediaCodec: MediaCodec, outBufferIndex: Int, bufferInfo: MediaCodec.BufferInfo)

    fun formatChanged(mediaCodec: MediaCodec, mediaFormat: MediaFormat)

}