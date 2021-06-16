package com.magi.adlive.live

import android.media.MediaCodec
import android.media.MediaFormat
import com.magi.adlive.ADLiveView
import com.magi.adlive.encode.Frame
import java.nio.ByteBuffer

class ADLivePusher(liveView: ADLiveView): ADRecorderBase(liveView) {

    override fun prepareAudioRtp(isStereo: Boolean, sampleRate: Int) {

    }

    override fun getAacData(aacBuffer: ByteBuffer, info: MediaCodec.BufferInfo) {

    }

    override fun onAudioFormat(mediaFormat: MediaFormat) {

    }

    override fun onSpsPpsVps(sps: ByteBuffer, pps: ByteBuffer, vps: ByteBuffer?) {

    }

    override fun getVideoData(h264Buffer: ByteBuffer, info: MediaCodec.BufferInfo) {

    }

    override fun onVideoFormat(mediaFormat: MediaFormat) {

    }

    override fun inputPCMData(frame: Frame) {

    }
}