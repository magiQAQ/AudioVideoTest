package com.magi.adlive.live

import android.content.Context
import com.magi.adlive.ADLiveView
import com.magi.adlive.encode.audio.GetAacData
import com.magi.adlive.encode.audio.GetMicrophoneData
import com.magi.adlive.encode.video.ADCameraController
import com.magi.adlive.encode.video.GetVideoData
import com.magi.adlive.encode.video.VideoEncoder
import com.magi.adlive.widget.ADLiveGLInterface

abstract class ADRecorderBase: GetAacData, GetVideoData, GetMicrophoneData {
    private val TAG = "ADRecorderBase"

    private val context: Context
    private val glInterface: ADLiveGLInterface
    private val cameraController: ADCameraController
    private val videoEncoder: VideoEncoder

    constructor(liveView: ADLiveView) {
        context = liveView.context
        glInterface = liveView
        glInterface.init()
        cameraController = ADCameraController(context)

    }
}