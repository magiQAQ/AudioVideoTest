package com.magi.adlive.encode.video

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.util.Log
import android.view.Surface
import com.magi.adlive.encode.BaseEncoder
import com.magi.adlive.encode.H264_MIME
import com.magi.adlive.encode.getAllHardwareEncoders
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
    private var formatVideoEncoder: FormatVideoEncoder? = FormatVideoEncoder.YUV420Dynamical
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
            if (encoder != null) {
                ADLogUtil.logD(TAG, "Encoder selected ${encoder.name}")
                codec = MediaCodec.createByCodecName(encoder.name)
                if (this.formatVideoEncoder == FormatVideoEncoder.YUV420Dynamical) {
                    this.formatVideoEncoder = chooseColorDynamically(encoder)
                    if (this.formatVideoEncoder == null) {
                        ADLogUtil.logE(TAG, "YUV420 dynamical choose failed")
                        return false
                    }
                }
            } else {
                ADLogUtil.logE(TAG, "Valid encoder not found")
                return false
            }
            // TODO: 2021/6/13
            
            
            return true
        } catch (e: Exception) {
            ADLogUtil.logE(TAG, "create VideoEncoder failed", e)
            stop()
            return false
        }
    }

    override fun chooseEncoder(mime: String): MediaCodecInfo? {
        val mediaCodecInfoList = getAllHardwareEncoders(mime)
        ADLogUtil.logD(TAG, "Found encoder $mime count ${mediaCodecInfoList.size}")
        val cbrPriority = ArrayList<MediaCodecInfo>()
        for (mci in mediaCodecInfoList) {
            if (isCBRModeSupported(mci)) {
                cbrPriority.add(mci)
            }
        }
        mediaCodecInfoList.removeAll(cbrPriority)
        mediaCodecInfoList.addAll(cbrPriority)
        for (mci in mediaCodecInfoList) {
            ADLogUtil.logD(TAG, "Encoder ${mci.name}")
            val codecCapabilities = mci.getCapabilitiesForType(mime)
            for (format in codecCapabilities.colorFormats) {
                ADLogUtil.logD(TAG, "format support: $format")
                if (formatVideoEncoder == FormatVideoEncoder.SURFACE) {
                    if (format == FormatVideoEncoder.SURFACE.getFormatCodec()) return mci
                } else {
                    //check if encoder support any yuv420 color
                    if (format == FormatVideoEncoder.YUV420PLANAR.getFormatCodec()
                        || format == FormatVideoEncoder.YUV420SEMIPLANAR.getFormatCodec()) {
                        return mci
                    }
                }
            }
        }
        return null
    }

    private fun isCBRModeSupported(mediaCodecInfo: MediaCodecInfo): Boolean {
        val codecCapabilities = mediaCodecInfo.getCapabilitiesForType(type)
        val encoderCapabilities = codecCapabilities.encoderCapabilities
        return encoderCapabilities.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
    }

    private fun chooseColorDynamically(mediaCodecInfo: MediaCodecInfo): FormatVideoEncoder? {
        for (format in mediaCodecInfo.getCapabilitiesForType(type).colorFormats) {
            if (format == FormatVideoEncoder.YUV420PLANAR.getFormatCodec()) {
                return FormatVideoEncoder.YUV420PLANAR
            } else if (format == FormatVideoEncoder.YUV420SEMIPLANAR.getFormatCodec()) {
                return FormatVideoEncoder.YUV420SEMIPLANAR
            }
        }
        return null
    }
}