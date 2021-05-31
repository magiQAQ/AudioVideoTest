package me.magi.media.video

import android.hardware.camera2.CameraCharacteristics
import android.os.*
import android.util.Size
import me.magi.media.model.MediaCodecGLWrapper
import me.magi.media.model.OffScreenGLWrapper
import me.magi.media.model.ScreenGLWrapper
import me.magi.media.utils.*
import java.nio.FloatBuffer
import java.nio.ShortBuffer

internal class ADVideoCore(private val config: ADVideoConfig) {

    private val mVideoThread: HandlerThread
    private val mVideoHandler: ADVideoHandler

    private val loopDurationMs = 1000 / config.targetFps
    init {
        val videoThread = HandlerThread("VideoThread")
        videoThread.start()
        val videoHandler = ADVideoHandler(videoThread.looper, config)
        mVideoThread = videoThread
        mVideoHandler = videoHandler
    }

    internal fun release() {
        mVideoHandler.removeCallbacksAndMessages(null)
        mVideoThread.quitSafely()
        mVideoThread.join()
    }

    // cameraTexture有可用帧时,该方法会被调用
    internal fun onFrameAvailable() {

    }

    internal class ADVideoHandler(looper: Looper, private val config: ADVideoConfig) : Handler(looper) {

        private var offScreenWrapper: OffScreenGLWrapper? = null
        private var screenWrapper: ScreenGLWrapper? = null
        private var mediaCodecWrapper: MediaCodecGLWrapper? = null

        private val syncCameraBuffer = Any()
        private val syncCamaraTexture = Any()
        private val syncFrameNum = Any()

        private lateinit var shapeVerticesBuffer: FloatBuffer
        private lateinit var mediaCodecTextureVerticesBuffer: FloatBuffer
        private lateinit var screenTextureVerticesBuffer: FloatBuffer
        private lateinit var drawIndicesBuffer: ShortBuffer
        private lateinit var cameraTextureVerticesBuffer: FloatBuffer
        private lateinit var camera2dTextureVerticesBuffer: FloatBuffer
        private var directionFlag: Int = 0

        private var screenSize = Size(1, 1)
        private var currentCamaraId = "0"

        init {
            initBuffer()
        }

        private fun initBuffer() {
            shapeVerticesBuffer = getShapeVerticesBuffer()
            mediaCodecTextureVerticesBuffer = getMediaCodecTextureVerticesBuffer()
            screenTextureVerticesBuffer = getScreenTextureVerticesBuffer()
            drawIndicesBuffer = getDrawIndicesBuffer()
            cameraTextureVerticesBuffer = getCameraTextureVerticesBuffer()
        }

        internal fun updateCameraFacing(cameraId: String) {
            synchronized(syncCameraBuffer) {
                currentCamaraId = cameraId
                directionFlag = getMode(cameraId)
                camera2dTextureVerticesBuffer = getCamera2DTextureVerticesBuffer(directionFlag, config.cropRatio)
            }
        }

        private fun getMode(cameraId: String): Int {
            val direction = ADCameraController.getCameraDirection(cameraId)
            val facing = ADCameraController.getCameraFacing(cameraId)
            return when (config.screenOrientation) {
                ADLiveConstant.ORIENTATION_PORTRAIT -> when (facing) {
                    CameraCharacteristics.LENS_FACING_FRONT -> when (direction) {
                        0 -> ADLiveConstant.FLAG_DIRECTION_ROTATION_180
                        90 -> ADLiveConstant.FLAG_DIRECTION_ROTATION_270
                        180 -> ADLiveConstant.FLAG_DIRECTION_ROTATION_0
                        270 -> ADLiveConstant.FLAG_DIRECTION_ROTATION_90
                        else -> 0
                    }
                    CameraCharacteristics.LENS_FACING_BACK -> when (direction) {
                        0 -> ADLiveConstant.FLAG_DIRECTION_ROTATION_0
                        90 -> ADLiveConstant.FLAG_DIRECTION_ROTATION_90
                        180 -> ADLiveConstant.FLAG_DIRECTION_ROTATION_180
                        270 -> ADLiveConstant.FLAG_DIRECTION_ROTATION_270
                        else -> 0
                    }
                    else -> 0
                }
                ADLiveConstant.ORIENTATION_LANDSCAPE -> when (facing) {
                    CameraCharacteristics.LENS_FACING_FRONT -> when (direction) {
                        0 -> ADLiveConstant.FLAG_DIRECTION_ROTATION_90
                        90 -> ADLiveConstant.FLAG_DIRECTION_ROTATION_180
                        180 -> ADLiveConstant.FLAG_DIRECTION_ROTATION_270
                        270 -> ADLiveConstant.FLAG_DIRECTION_ROTATION_0
                        else -> 0
                    }
                    CameraCharacteristics.LENS_FACING_BACK -> when (direction) {
                        0 -> ADLiveConstant.FLAG_DIRECTION_ROTATION_270
                        90 -> ADLiveConstant.FLAG_DIRECTION_ROTATION_0
                        180 -> ADLiveConstant.FLAG_DIRECTION_ROTATION_90
                        270 -> ADLiveConstant.FLAG_DIRECTION_ROTATION_180
                        else -> 0
                    }
                    else -> 0
                }
                else -> 0
            }
        }
    }
}