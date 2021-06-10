package com.magi.adlive.encode.audio

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

interface GetAacData {
    fun getAacData(aacBuffer: ByteBuffer, info: MediaCodec.BufferInfo)

    fun onAudioFormat(mediaFormat: MediaFormat)
}