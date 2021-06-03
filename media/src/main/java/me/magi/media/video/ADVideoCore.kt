package me.magi.media.video

import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCharacteristics
import android.opengl.EGL14
import android.opengl.GLES20
import android.os.*
import android.util.Size
import me.magi.media.filter.BaseHardVideoFilter
import me.magi.media.model.MediaCodecGLWrapper
import me.magi.media.model.OffScreenGLWrapper
import me.magi.media.model.ScreenGLWrapper
import me.magi.media.rtmp.ADFlvDataCollector
import me.magi.media.utils.*
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.IllegalStateException

internal class ADVideoCore(private val config: ADVideoConfig) {

    companion object{
        private const val WHAT_INIT = 0x001
        private const val WHAT_UN_INIT = 0x002
        private const val WHAT_FRAME = 0x003
        private const val WHAT_DRAW = 0x004
        private const val WHAT_SET_FILTER = 0x006
        private const val WHAT_START_PREVIEW = 0x010
        private const val WHAT_STOP_PREVIEW = 0x020
        private const val WHAT_START_STREAMING = 0x100
        private const val WHAT_STOP_STREAMING = 0x200
        private const val WHAT_RESET_VIDEO = 0x005
        private const val WHAT_RESET_BITRATE = 0x300
    }

    private val mVideoThread: HandlerThread
    private val mVideoHandler: ADVideoHandler
    private val syncObject = Any()
    private val syncLooping = Any()

    private val loopDurationMs = 1000 / config.targetFPS

    private var isPreview = false
    private var isStream = false

    init {
        val videoThread = HandlerThread("VideoThread")
        videoThread.start()
        val videoHandler = ADVideoHandler(videoThread.looper, config)
        mVideoThread = videoThread
        mVideoHandler = videoHandler
        mVideoHandler.sendEmptyMessage(WHAT_INIT)
    }

    internal fun release() {
        mVideoHandler.sendEmptyMessage(WHAT_UN_INIT)
        mVideoThread.quitSafely()
        try {
            mVideoThread.join()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    internal fun setCameraTexture(surfaceTexture: SurfaceTexture) {
        synchronized(syncObject){
            mVideoHandler.setCameraTexture(surfaceTexture)
        }
    }

    internal fun startPreview(surfaceTexture: SurfaceTexture, width: Int, height: Int) {
        synchronized(syncObject) {
            mVideoHandler.obtainMessage(WHAT_START_PREVIEW, width, height, surfaceTexture)
                .let { mVideoHandler.sendMessage(it) }
            synchronized(syncLooping) {
                if (!isPreview && !isStream) {
                    mVideoHandler.removeMessages(WHAT_DRAW)
                    mVideoHandler.obtainMessage(WHAT_DRAW, SystemClock.uptimeMillis() + loopDurationMs)
                        .let { mVideoHandler.sendMessageDelayed(it, loopDurationMs.toLong()) }
                }
                isPreview = true
            }
        }
    }

    internal fun updatePreview(width: Int, height: Int) {
        synchronized(syncObject) {
            mVideoHandler.updateScreenWidth(width, height)
        }
    }

    internal fun stopPreview(releaseTexture: Boolean) {
        synchronized(syncObject) {
            mVideoHandler.obtainMessage(WHAT_STOP_PREVIEW, releaseTexture)
                .let { mVideoHandler.sendMessage(it) }
            synchronized(syncLooping) {
                isPreview = false
            }
        }
    }

    internal fun startStreaming(flvDataCollector: ADFlvDataCollector) {
        synchronized(syncObject) {
            mVideoHandler.obtainMessage(WHAT_START_STREAMING, flvDataCollector)
                .let { mVideoHandler.sendMessage(it) }
            synchronized(syncLooping) {
                if (!isPreview && !isStream) {
                    mVideoHandler.removeMessages(WHAT_DRAW)
                    mVideoHandler.obtainMessage(WHAT_DRAW, SystemClock.uptimeMillis() + loopDurationMs)
                        .let { mVideoHandler.sendMessageDelayed(it, loopDurationMs.toLong()) }
                }
                isStream = true
            }
        }
    }

    internal fun stopStreaming() {
        synchronized (syncObject) {
            mVideoHandler.sendEmptyMessage(WHAT_STOP_STREAMING)
            synchronized (syncLooping) {
                isStream = false
            }
        }
    }

    internal fun setVideoFilter(videoFilter: BaseHardVideoFilter) {
        mVideoHandler.obtainMessage(WHAT_SET_FILTER, videoFilter).let {
            mVideoHandler.sendMessage(it)
        }
    }

    // cameraTexture有可用帧时,该方法会被调用
    internal fun onFrameAvailable() {
        mVideoHandler.addFrameNum()
    }

    internal fun setCurrentCamera(cameraId: String) {
        mVideoHandler.setCameraId(cameraId)
    }

    private class ADVideoHandler(looper: Looper, private val config: ADVideoConfig) : Handler(looper) {

        private var mCameraTexture: SurfaceTexture? = null
        private var mScreenTexture: SurfaceTexture? = null
        private var mInnerVideoFilter: BaseHardVideoFilter? = null

        private var mOffScreenWrapper: OffScreenGLWrapper? = null
        private var mScreenWrapper: ScreenGLWrapper? = null
        private var mMediaCodecWrapper: MediaCodecGLWrapper? = null

        private val syncCameraBuffer = Any()
        private val syncCamaraTexture = Any()
        private val syncFrameNum = Any()
        private val syncFilter = Any()

        private lateinit var shapeVerticesBuffer: FloatBuffer
        private lateinit var mediaCodecTextureVerticesBuffer: FloatBuffer
        private lateinit var screenTextureVerticesBuffer: FloatBuffer
        private lateinit var drawIndicesBuffer: ShortBuffer
        private lateinit var cameraTextureVerticesBuffer: FloatBuffer
        private lateinit var camera2dTextureVerticesBuffer: FloatBuffer
        private var sample2DFrameBuffer = 0
        private var sample2DFrameBufferTexture = 0
        private var frameBuffer = 0
        private var frameBufferTexture = 0

        private var directionFlag: Int = 0
        private var mScreenSize = Size(1, 1)
        private var mCurrentCamaraId = "0"
        private var mFrameNum = 0
        private var mDropNextFrame = false

        init {
            initBuffer()
        }

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            when(msg.what) {
                WHAT_INIT -> {

                }
                WHAT_SET_FILTER -> {
                    synchronized(syncFilter) {
                        val filter = msg.obj as BaseHardVideoFilter
                        if (mInnerVideoFilter != filter) {
                            mInnerVideoFilter?.onDestroy()
                            mInnerVideoFilter = filter
                            mInnerVideoFilter?.onInit(config.videoHeight, config.videoWidth)
                        }
                    }
                }
            }
        }

        private fun initBuffer() {
            shapeVerticesBuffer = getShapeVerticesBuffer()
            mediaCodecTextureVerticesBuffer = getMediaCodecTextureVerticesBuffer()
            screenTextureVerticesBuffer = getScreenTextureVerticesBuffer()
            drawIndicesBuffer = getDrawIndicesBuffer()
            cameraTextureVerticesBuffer = getCameraTextureVerticesBuffer()
            setCameraId(mCurrentCamaraId)
        }

        private fun initOffScreenGLWrapper() {
            if (mOffScreenWrapper == null) {
                val offScreenGLWrapper = OffScreenGLWrapper()
                initOffScreenGL(offScreenGLWrapper)
                makeCurrent(offScreenGLWrapper)
                // camera
                offScreenGLWrapper.cameraProgram = createCameraProgram()
                GLES20.glUseProgram(offScreenGLWrapper.cameraProgram)
                offScreenGLWrapper.cameraTextureLoc = GLES20.glGetUniformLocation(
                    offScreenGLWrapper.cameraProgram, "uTexture"
                )
                offScreenGLWrapper.cameraPositionLoc =GLES20.glGetAttribLocation(
                    offScreenGLWrapper.cameraProgram, "aPosition"
                )
                offScreenGLWrapper.cameraTextureCoordLoc = GLES20.glGetAttribLocation(
                    offScreenGLWrapper.cameraProgram, "aTextureCoord"
                )
                // camera2d
                offScreenGLWrapper.camera2dProgram = createCamera2DProgram()
                GLES20.glUseProgram(offScreenGLWrapper.camera2dProgram)
                offScreenGLWrapper.camera2dTextureLoc = GLES20.glGetUniformLocation(
                    offScreenGLWrapper.camera2dTextureLoc, "uTexture"
                )
                offScreenGLWrapper.camera2dPositionLoc = GLES20.glGetAttribLocation(
                    offScreenGLWrapper.camera2dPositionLoc, "aPosition"
                )
                offScreenGLWrapper.camera2dTextureCoordLoc = GLES20.glGetAttribLocation(
                    offScreenGLWrapper.camera2dProgram, "aTextureCoord"
                )
                offScreenGLWrapper.camera2dTextureMatrix = GLES20.glGetUniformLocation(
                    offScreenGLWrapper.camera2dProgram, "uTextureMatrix"
                )
                val fb = IntArray(1)
                val fbt = IntArray(1)
                createCameraFrameBuffer(fb, fbt, config.previewHeight, config.previewWidth)
                sample2DFrameBuffer = fb[0]
                sample2DFrameBufferTexture = fbt[0]
                createCameraFrameBuffer(fb, fbt, config.previewHeight, config.previewWidth)
                frameBuffer = fb[0]
                frameBufferTexture = fbt[0]
                mOffScreenWrapper = offScreenGLWrapper
            } else {
                throw IllegalStateException("offScreenGL has been initialized")
            }
        }

        private fun releaseOffScreenGLWrapper() {
            val offScreenGLWrapper = mOffScreenWrapper
            if (offScreenGLWrapper != null) {
                makeCurrent(offScreenGLWrapper)
                GLES20.glDeleteProgram(offScreenGLWrapper.cameraProgram)
                GLES20.glDeleteProgram(offScreenGLWrapper.camera2dProgram)
                GLES20.glDeleteFramebuffers(1, intArrayOf(frameBuffer), 0)
                GLES20.glDeleteTextures(1, intArrayOf(frameBufferTexture), 0)
                GLES20.glDeleteFramebuffers(1, intArrayOf(sample2DFrameBuffer), 0)
                GLES20.glDeleteTextures(1, intArrayOf(sample2DFrameBufferTexture), 0)
                EGL14.eglDestroySurface(offScreenGLWrapper.eglDisplay, offScreenGLWrapper.eglSurface)
                EGL14.eglDestroyContext(offScreenGLWrapper.eglDisplay, offScreenGLWrapper.eglContext)
                EGL14.eglTerminate(offScreenGLWrapper.eglDisplay)
                EGL14.eglMakeCurrent(offScreenGLWrapper.eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
                mOffScreenWrapper = null
            } else {
                throw IllegalStateException("offScreenGL has not been initialized")
            }
        }


        fun setCameraId(cameraId: String) {
            synchronized(syncCameraBuffer) {
                mCurrentCamaraId = cameraId
                directionFlag = getMode(cameraId)
                camera2dTextureVerticesBuffer = getCamera2DTextureVerticesBuffer(directionFlag, config.cropRatio)
            }
        }

        fun setCameraTexture(cameraTexture: SurfaceTexture) {
            synchronized(syncCamaraTexture) {
                if (mCameraTexture != cameraTexture) {
                    mCameraTexture = cameraTexture
                    mFrameNum = 0
                    mDropNextFrame = false
                }
            }
        }

        fun updateScreenWidth(width: Int, height: Int) {
            mScreenSize = Size(width, height)
        }

        fun addFrameNum() {
            synchronized(syncFrameNum) {
                ++mFrameNum
                removeMessages(WHAT_FRAME)
                sendMessageAtFrontOfQueue(obtainMessage(WHAT_FRAME))
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