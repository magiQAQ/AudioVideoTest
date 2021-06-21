package com.magi.adlive.live

import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.media.MediaRecorder
import android.util.Log
import com.magi.adlive.ADLiveView
import com.magi.adlive.encode.Frame
import com.magi.adlive.encode.audio.ADAudioEncoder
import com.magi.adlive.encode.audio.ADMicrophoneController
import com.magi.adlive.encode.audio.GetAacData
import com.magi.adlive.encode.audio.GetMicrophoneData
import com.magi.adlive.encode.video.*
import com.magi.adlive.encode.video.ADCameraController
import com.magi.adlive.model.Facing
import com.magi.adlive.util.ADLogUtil
import com.magi.adlive.util.getCameraOrientation
import com.magi.adlive.util.isPortrait
import com.magi.adlive.widget.ADLiveGLInterface
import java.nio.ByteBuffer

abstract class ADRecorderBase: GetAacData, GetVideoData, GetMicrophoneData {
    private val TAG = "ADRecorderBase"

    private val context: Context
    private var glInterface: ADLiveGLInterface
    private val cameraController: ADCameraController
    private val microphoneController: ADMicrophoneController
    protected val videoEncoder: ADVideoEncoder
    protected val audioEncoder: ADAudioEncoder

    private var isPreview = false
    private var isStreaming = false
    private var audioInitialized = false

    private var previewWidth = 0
    private var previewHeight = 0


    constructor(liveView: ADLiveView) {
        context = liveView.context
        glInterface = liveView
        glInterface.init()
        cameraController = ADCameraController(context)
        microphoneController = ADMicrophoneController(this)
        videoEncoder = ADVideoEncoder(this)
        audioEncoder = ADAudioEncoder(this)
    }

    fun setCameraCallback(callback: ADCameraCallback) {
        cameraController.setCameraCallback(callback)
    }

    fun getCameraFacing(): Facing {
        return cameraController.facing
    }

    fun prepareVideo(width: Int = 1280, height: Int = 720, fps: Int = 30, bitrate: Int = 4000 * 1024, gop: Int = 3): Boolean{
        val rotation = getCameraOrientation(context)
        if (isPreview && !(width == previewWidth && height == previewHeight)) {
            stopPreview()
            isPreview = true
        }
        return videoEncoder.prepareVideoEncoder(width, height, fps, bitrate, rotation, gop,
            FormatVideoEncoder.SURFACE, MediaCodecInfo.CodecProfileLevel.AVCProfileBaseline,
            /**自动**/-1)
    }

    fun prepareAudio(audioSource: Int = MediaRecorder.AudioSource.DEFAULT, sampleRate: Int = 44100,
                     channelCount: Int = 2, bitrate: Int = 64 * 1024, echoCancel: Boolean = false, autoGain: Boolean = false,
                     noiseSuppress: Boolean = false): Boolean {
        if (!microphoneController.createMicrophone(audioSource, sampleRate, channelCount, echoCancel, autoGain, noiseSuppress)) {
            return false
        }
        prepareAudioRtp(channelCount == 2, sampleRate)
        return audioEncoder.prepareAudioEncoder(bitrate, sampleRate, channelCount == 2, microphoneController.getMaxInputSize())
    }

    fun startPreview(cameraFacing: Facing, width: Int, height: Int, fps: Int) {
        val glInterface = this.glInterface
        if (!isStreaming && !isPreview) {
            previewWidth = width
            previewHeight = height
            if (isPortrait(context)) {
                glInterface.setEncoderSize(height, width)
            } else {
                glInterface.setEncoderSize(width, height)
            }
            val rotation = getCameraOrientation(context)
            glInterface.setRotation(if (rotation == 0) 270 else rotation - 90)
            glInterface.start()
            cameraController.prepareCamera(glInterface.getSurfaceTexture(), width, height, fps)
            cameraController.openCameraFacing(cameraFacing)
            isPreview = true
        } else {
            ADLogUtil.logE(TAG, "camera is already preview")
        }
    }

    /**
     * 调用此方法前确保已经不处于推流状态
     */
    fun stopPreview() {
        if (isPreview && !isStreaming) {
            glInterface.stop()
            cameraController.closeCamera()
            isPreview = false
            previewWidth = 0
            previewHeight = 0
        } else {
            if (isStreaming) {
                ADLogUtil.logE(TAG, "you need call stopStream() before call stopPreview() ")
            }
            if (!isPreview) {
                ADLogUtil.logE(TAG, "preview is already stopped")
            }
        }
    }

    fun startStreaming(url: String) {
        isStreaming = true
        startEncoders()
        startStreamRtp(url)
        isPreview = true
    }

    fun stopStreaming() {
        if (isStreaming) {
            stopStreamRtp()
            isStreaming = false
        }
        isPreview = true
        microphoneController.stop()
        glInterface.removeMediaCodecSurface()
        stopEncoders()
    }

    protected abstract fun prepareAudioRtp(isStereo: Boolean, sampleRate: Int)

    private fun startEncoders() {
        videoEncoder.start()
        audioEncoder.start()
        microphoneController.start()
        prepareGLView()
        isPreview = true
    }

    private fun stopEncoders() {
        videoEncoder.stop()
        audioEncoder.stop()
    }

    private fun prepareGLView() {
        glInterface.setFps(videoEncoder.fps)
        if (videoEncoder.rotation == 90 || videoEncoder.rotation == 270) {
            glInterface.setEncoderSize(videoEncoder.height, videoEncoder.width)
        } else {
            glInterface.setEncoderSize(videoEncoder.width, videoEncoder.height)
        }
        glInterface.setRotation(if (videoEncoder.rotation == 0) 270 else videoEncoder.rotation - 90)
        val inputSurface = videoEncoder.inputSurface
        if (inputSurface != null) {
            glInterface.addMediaCodecSurface(inputSurface)
        }
        cameraController.prepareCamera(glInterface.getSurfaceTexture(), videoEncoder.width, videoEncoder.height, videoEncoder.fps)
    }

    protected abstract fun startStreamRtp(url: String)

    protected abstract fun stopStreamRtp()

    fun retry(delay: Long, reason: String, backUrl: String? = null): Boolean {
        val result = shouldRetry(reason)
        if (result) {
            videoEncoder.requestKeyframe()
            reConnect(delay, backUrl)
        }
        return result
    }

    protected abstract fun shouldRetry(reason: String): Boolean

    abstract fun setRetries(retries: Int)

    protected abstract fun reConnect(delay: Long, backUrl: String?)

    abstract fun hasCongestion(): Boolean

    abstract fun resizeCache(newSize: Int)

    abstract fun getCacheSize(): Int

    abstract fun getSentAudioFrames(): Long

    abstract fun getSentVideoFrames(): Long

    abstract fun getDroppedAudioFrames(): Long

    abstract fun getDroppedVideoFrames(): Long

    abstract fun resetSentAudioFrames(): Long

    abstract fun resetSentVideoFrames(): Long

    abstract fun resetDroppedAudioFrames(): Long

    abstract fun resetDroppedVideoFrames(): Long

    abstract fun setAuthorization(user: String, password: String)

    fun switchCamera() {
        if (isPreview) {
            cameraController.switchCamera()
        }
    }

    fun muteAudio(isMute: Boolean) {
        microphoneController.isMuted = isMute
    }

    override fun inputPCMData(frame: Frame) {
        audioEncoder.inputPCMData(frame)
    }

    override fun onVideoFormat(mediaFormat: MediaFormat) {}

    override fun onAudioFormat(mediaFormat: MediaFormat) {}

    override fun getAacData(aacBuffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (isStreaming) getAacDataRtp(aacBuffer, info)
    }

    protected abstract fun getAacDataRtp(aacBuffer: ByteBuffer, info: MediaCodec.BufferInfo)

    override fun onSpsPpsVps(sps: ByteBuffer, pps: ByteBuffer, vps: ByteBuffer?) {
        if (isStreaming) onSpsPpsVpsRtp(sps.duplicate(), pps.duplicate(), vps?.duplicate())
    }

    protected abstract fun onSpsPpsVpsRtp(sps: ByteBuffer, pps: ByteBuffer, vps: ByteBuffer?)

    override fun getVideoData(h264Buffer: ByteBuffer, info: MediaCodec.BufferInfo) {
        if (isStreaming) getH264DataRtp(h264Buffer, info)
    }

    protected abstract fun getH264DataRtp(h264Buffer: ByteBuffer, info: MediaCodec.BufferInfo)

}