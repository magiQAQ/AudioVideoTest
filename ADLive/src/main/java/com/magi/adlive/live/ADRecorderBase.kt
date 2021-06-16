package com.magi.adlive.live

import android.content.Context
import android.media.MediaRecorder
import android.util.Log
import com.magi.adlive.ADLiveView
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

abstract class ADRecorderBase: GetAacData, GetVideoData, GetMicrophoneData {
    private val TAG = "ADRecorderBase"

    private val context: Context
    private var glInterface: ADLiveGLInterface? = null
    private val cameraController: ADCameraController
    private val microphoneController: ADMicrophoneController
    private val videoEncoder: ADVideoEncoder
    private val audioEncoder: ADAudioEncoder

    private var isPreview = false
    private var isStreaming = false
    private var audioInitialized = false

    private var previewWidth = 0
    private var previewHeight = 0


    constructor(liveView: ADLiveView) {
        context = liveView.context
        glInterface = liveView
        glInterface?.init()
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

    fun prepareVideo(width: Int = 1280, height: Int = 720, fps: Int = 30, bitrate: Int = 4000 * 1024,
                    gop: Int = 3, avcProfile: Int = -1, avcProfileLevel: Int = -1): Boolean{
        val rotation = getCameraOrientation(context)
        if (isPreview && !(glInterface != null && width == previewWidth && height == previewHeight)) {
            stopPreview()
            isPreview = true
        }
        return videoEncoder.prepareVideoEncoder(width, height, fps, bitrate, rotation, gop,
            FormatVideoEncoder.SURFACE, avcProfile, avcProfileLevel)
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

    fun startPreview(cameraFacing: Facing, width: Int, height: Int, rotation: Int) {
        val glInterface = this.glInterface ?: return
        if (!isStreaming && !isPreview) {
            previewWidth = width
            previewHeight = height
            if (isPortrait(context)) {
                glInterface.setEncoderSize(height, width)
            } else {
                glInterface.setEncoderSize(width, height)
            }
            glInterface.setRotation(if (rotation == 0) 270 else rotation - 90)
            glInterface.start()
            cameraController.prepareCamera(glInterface.getSurfaceTexture(), width, height, videoEncoder.fps)
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
            glInterface?.stop()
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

    protected abstract fun prepareAudioRtp(isStereo: Boolean, sampleRate: Int)
}