package com.magi.adlive.encode.video

import android.media.MediaCodecInfo
import android.view.Surface
import com.magi.adlive.encode.BaseEncoder
import com.magi.adlive.encode.H264_MIME
import com.magi.adlive.util.ADFpsLimiter
import com.magi.adlive.util.ADLogUtil

class VideoEncoder(private val getVideoData: GetVideoData): BaseEncoder(), GetCameraData {

    private var spsPpsSett = false
    private var forceKey = false

    private lateinit var inputSurface: Surface
    private var width = 1280
    private var height = 720
    private var fps = 30
    private var bitrate = 3000 * 1024
    private var rotation = 90
    private var iFrameInterval = 2

    private var fpsLimiter = ADFpsLimiter()
    private var type = H264_MIME
    private var formatVideoEncoder = FormatVideoEncoder.YUV420Dynamical
    private var avcProfile = -1
    private var avcProfileLevel = -1


    init {
        TAG = "VideoEncoder"
    }

    fun prepareVideoEncoder(width: Int, height: Int, fps: Int, bitrate: Int, rotation: Int,
                            iFrameInterval: Int, formatVideoEncoder: FormatVideoEncoder,
                            avcProfile: Int = -1, avcProfileLevel: Int = -1): Boolean {
        this.width = width
        this.height = height
        this.fps = fps
        this.bitrate = bitrate
        this.rotation = rotation
        this.formatVideoEncoder = formatVideoEncoder
        this.avcProfile = avcProfile
        this.avcProfileLevel = avcProfileLevel
        isBufferMode = true
        val encoder = chooseEncoder(type)
        try {

            return true
        } catch (e: Exception) {
            ADLogUtil.logE(TAG, "create VideoEncoder failed", e)
            stop()
            return false
        }
    }

    override fun chooseEncoder(mime: String): MediaCodecInfo {
        TODO("Not yet implemented")
    }
}