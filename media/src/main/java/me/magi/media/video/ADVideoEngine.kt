package me.magi.media.video

import android.graphics.SurfaceTexture

class ADVideoEngine(private val config: ADVideoConfig) {

    companion object{
        private const val CAMERA_TEXTURE_ID = 1000
    }

    private var isPreviewing = false
    private var isStreaming = false
    private var isRecording = false
    private var videoCore = ADVideoCore(config)
    private var cameraController = ADCameraController()
    private var cameraTexture: SurfaceTexture
    private var syncObject = Any()

    init {
        cameraController.setCamera(config.cameraFacing, config.cameraIndex)
        cameraController.setSize(config.targetWidth, config.targetHeight).let {
            config.previewWidth = it.width
            config.previewHeight = it.height
        }
        cameraController.setFps(config.targetFps).let {
            config.previewMaxFPS = it.upper
            config.previewMinFPS = it.upper
        }
        cameraTexture = generateCameraTexture()
        cameraController.setSurfaceTexture(cameraTexture)
    }

    private fun generateCameraTexture():SurfaceTexture {
        val surfaceTexture = SurfaceTexture(CAMERA_TEXTURE_ID)
        surfaceTexture.setOnFrameAvailableListener {
            synchronized(syncObject) {
                videoCore.onFrameAvailable()
            }
        }
        return surfaceTexture
    }

    fun release(){
        cameraController.release()
        videoCore.release()
    }


}