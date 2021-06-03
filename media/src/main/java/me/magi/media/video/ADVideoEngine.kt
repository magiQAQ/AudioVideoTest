package me.magi.media.video

import android.graphics.SurfaceTexture
import me.magi.media.utils.ADLiveConstant

class ADVideoEngine(private val config: ADVideoConfig) {

    companion object{
        private const val CAMERA_TEXTURE_ID = 1000
    }

    private var isPreviewing = false
    private var isStreaming = false
    private var isRecording = false
    private var mVideoCore : ADVideoCore
    private var mCameraController = ADCameraController()
    private lateinit var mCameraTexture: SurfaceTexture
    private var syncObject = Any()

    init {
        mCameraController.setCamera(config.cameraFacing, config.cameraIndex)
        mCameraController.setSize(config.targetWidth, config.targetHeight).let {
            config.previewWidth = it.width
            config.previewHeight = it.height
        }
        mCameraController.setFps(config.targetFPS).let {
            config.previewMaxFPS = it.upper
            config.previewMinFPS = it.upper
        }
        updateVideoRatio()
        mVideoCore = ADVideoCore(config)
    }

    private fun generateCameraTexture():SurfaceTexture {
        val surfaceTexture = SurfaceTexture(CAMERA_TEXTURE_ID)
        surfaceTexture.setOnFrameAvailableListener {
            synchronized(syncObject) {
                mVideoCore.onFrameAvailable()
            }
        }
        return surfaceTexture
    }

    /**
     * 摄像头拍摄的画面有可能与用户选择的分辨率不符,因此需要裁剪画面
     */
    private fun updateVideoRatio() {
        val pw: Float
        val ph: Float
        if (config.screenOrientation == ADLiveConstant.ORIENTATION_PORTRAIT) {
            config.videoWidth = config.targetHeight
            config.videoHeight = config.targetWidth
            pw = config.previewHeight.toFloat()
            ph = config.previewWidth.toFloat()
        } else {
            config.videoWidth = config.targetWidth
            config.videoHeight = config.targetHeight
            pw = config.previewWidth.toFloat()
            ph = config.previewHeight.toFloat()
        }
        val vw: Float = config.videoWidth.toFloat()
        val vh: Float = config.videoHeight.toFloat()
        val pr = ph / pw
        val vr = vh / vw
        config.cropRatio = when {
            pr > vr -> (1f - vr / pr) / 2f
            pr < vr -> -(1f - pr / vr) /2f
            else -> 0f
        }
    }

    fun startPreview(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        synchronized(syncObject) {
            mCameraTexture = generateCameraTexture()
            mCameraController.setSurfaceTexture(mCameraTexture)
            if (!mCameraController.isCameraOpen()) {
                mCameraController.openCamera()
            }
            mVideoCore.setCameraTexture(mCameraTexture)
            mVideoCore.startPreview(surfaceTexture, width, height)
        }
    }

    fun release(){
        mCameraController.release()
        mVideoCore.release()
    }


}