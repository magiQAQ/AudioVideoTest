package com.magi.adlive.encode.video

import android.media.MediaCodec
import android.media.MediaFormat
import java.nio.ByteBuffer

interface GetVideoData {

    fun onSpsPpsVps(sps: ByteBuffer, pps: ByteBuffer, vps: ByteBuffer?)

    fun getVideoData(h264Buffer: ByteBuffer, info: MediaCodec.BufferInfo)

    fun onVideoFormat(mediaFormat: MediaFormat)

}