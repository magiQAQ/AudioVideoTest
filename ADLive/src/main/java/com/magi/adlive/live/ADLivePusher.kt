package com.magi.adlive.live

import android.media.MediaCodec
import com.magi.adlive.ADLiveView
import com.magi.adlive.librtmp.rtmp.RtmpClient
import com.magi.adlive.librtmp.utils.ConnectCheckerRtmp
import com.magi.adlive.util.ADLogUtil
import java.nio.ByteBuffer

class ADLivePusher(liveView: ADLiveView, connectCheckerRtmp: ConnectCheckerRtmp): ADRecorderBase(liveView) {

    private val TAG = javaClass.simpleName
    private val rtmpClient = RtmpClient(connectCheckerRtmp)


    override fun resizeCache(newSize: Int) {
        try {
            rtmpClient.resizeCache(newSize)
        } catch (e: RuntimeException) {
            ADLogUtil.logE(TAG, e.message?:"", e)
        }
    }

    override fun getCacheSize(): Int {
        return rtmpClient.cacheSize
    }

    override fun getSentAudioFrames(): Long {
        return rtmpClient.sentAudioFrames
    }

    override fun getSentVideoFrames(): Long {
        return rtmpClient.sentVideoFrames
    }

    override fun getDroppedAudioFrames(): Long {
        return rtmpClient.droppedAudioFrames
    }

    override fun getDroppedVideoFrames(): Long {
        return rtmpClient.droppedVideoFrames
    }

    override fun resetSentAudioFrames(): Long {
        return rtmpClient.sentAudioFrames
    }

    override fun resetSentVideoFrames(): Long {
        return rtmpClient.sentVideoFrames
    }

    override fun resetDroppedAudioFrames(): Long {
        return rtmpClient.droppedAudioFrames
    }

    override fun resetDroppedVideoFrames(): Long {
        return rtmpClient.droppedVideoFrames
    }

    override fun setAuthorization(user: String, password: String) {
        rtmpClient.setAuthorization(user, password)
    }

    override fun prepareAudioRtp(isStereo: Boolean, sampleRate: Int) {
        rtmpClient.setAudioInfo(sampleRate, isStereo)
    }

    override fun startStreamRtp(url: String) {
        if (videoEncoder.rotation == 90 || videoEncoder.rotation == 270) {
            rtmpClient.setVideoResolution(videoEncoder.height, videoEncoder.width)
        } else {
            rtmpClient.setVideoResolution(videoEncoder.width, videoEncoder.height)
        }
        rtmpClient.connect(url)
    }

    override fun stopStreamRtp() {
        rtmpClient.disconnect()
    }

    override fun shouldRetry(reason: String): Boolean {
        return rtmpClient.shouldRetry(reason)
    }

    override fun setRetries(retries: Int) {
        rtmpClient.setReTries(retries)
    }

    override fun reConnect(delay: Long, backUrl: String?) {
        rtmpClient.reConnect(delay, backUrl)
    }

    override fun hasCongestion(): Boolean {
        return rtmpClient.hasCongestion()
    }

    override fun getAacDataRtp(aacBuffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        rtmpClient.sendAudio(aacBuffer, info)
    }

    override fun onSpsPpsVpsRtp(sps: ByteBuffer, pps: ByteBuffer, vps: ByteBuffer?) {
        rtmpClient.setVideoInfo(sps, pps, vps)
    }

    override fun getH264DataRtp(h264Buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        rtmpClient.sendVideo(h264Buffer, info)
    }
}