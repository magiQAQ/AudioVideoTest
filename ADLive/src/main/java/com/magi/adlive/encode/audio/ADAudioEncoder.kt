package com.magi.adlive.encode.audio

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import com.magi.adlive.encode.*
import com.magi.adlive.util.ADLogUtil
import java.nio.ByteBuffer

class ADAudioEncoder(private val getAacData: GetAacData): BaseEncoder(), GetMicrophoneData {

    private var bitrate = 64 * 1024
    private var sampleRate = 44100
    private var maxInputSize = 0
    private var isStereo = true

    init {
        TAG = "ADAudioEncoder"
    }

    fun prepareAudioEncoder(bitrate: Int = 64 * 1024, sampleRate: Int = 44100, isStereo: Boolean = true,
                            maxInputSize: Int = 0): Boolean {
        this.bitrate = bitrate
        this.sampleRate = sampleRate
        this.maxInputSize = maxInputSize
        this.isStereo = isStereo
        isBufferMode = true
        try {
            val encoder = chooseEncoder(AAC_MIME)
            if (encoder != null) {
                ADLogUtil.logD(TAG, "Encoder selected ${encoder.name}")
                codec = MediaCodec.createByCodecName(encoder.name)
            } else {
                ADLogUtil.logE(TAG, "Valid encoder not found")
                return false
            }

            val channelCount = if (isStereo) 2 else 1
            val audioFormat = MediaFormat.createAudioFormat(AAC_MIME, sampleRate, channelCount)
            audioFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            audioFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, maxInputSize)
            audioFormat.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            codec.configure(audioFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            isRunning = false
            ADLogUtil.logD(TAG, "audio encoder prepared")
            return true
        } catch (e: Exception) {
            ADLogUtil.logE(TAG, "Create AudioEncoder failed.", e)
            stop()
            return false
        }
    }

    override fun start(resetTs: Boolean) {
        shouldReset = resetTs
        ADLogUtil.logD(TAG, "started")
    }

    override fun stopImp() {
        ADLogUtil.logD(TAG, "stopped")
    }

    override fun reset() {
        stop(false)
        prepareAudioEncoder(bitrate, sampleRate, isStereo, maxInputSize)
        restart()
    }

    override fun getInputFrame(): Frame? {
        return queue.take()
    }

    override fun checkBuffer(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        fixTimeStamp(bufferInfo)
    }

    override fun sendBuffer(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        getAacData.getAacData(byteBuffer, bufferInfo)
    }

    override fun inputPCMData(frame: Frame) {
        if (isRunning && !queue.offer(frame)) {
            ADLogUtil.logD(TAG, "frame discarded")
        }
    }

    override fun chooseEncoder(mime: String): MediaCodecInfo? {
        val mediaCodecInfoList = getAllHardwareEncoders(mime)
        ADLogUtil.logD(TAG, "Found encoder $mime count ${mediaCodecInfoList.size}")

        for (mci in mediaCodecInfoList) {
            val name = mci.name.lowercase()
            if (name.contains("omx.google") && mediaCodecInfoList.size > 1) {
                continue
            }
            return mci
        }
        return null
    }

    override fun formatChanged(mediaCodec: MediaCodec, mediaFormat: MediaFormat) {
        getAacData.onAudioFormat(mediaFormat)
    }

}