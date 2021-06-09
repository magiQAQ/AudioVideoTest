package com.magi.adlive.widget

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import android.view.TextureView
import com.magi.adlive.ScreenShotCallback
import com.magi.adlive.gl.Filter
import com.magi.adlive.gl.SurfaceManager
import com.magi.adlive.util.ADLogUtil
import com.magi.adlive.util.ADFpsLimiter
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.Semaphore

abstract class ADLiveGLViewBase: TextureView, ADLiveGLInterface, Runnable, SurfaceTexture.OnFrameAvailableListener, TextureView.SurfaceTextureListener{

    private val TAG = "ADLiveGLViewBase"

    protected var glThread: Thread? = null
    protected var frameAvailable = false
    protected var running = false
    protected var initialized = false

    protected val surfaceManagerPhoto = SurfaceManager()
    protected val surfaceManager = SurfaceManager()
    protected val surfaceManagerEncoder = SurfaceManager()

    protected val fpsLimiter = ADFpsLimiter()
    protected val semaphore = Semaphore(0)
    protected val filterQueue = LinkedBlockingQueue<Filter>()
    protected val sync = Any()
    protected var previewWidth: Int = 0
    protected var previewHeight: Int = 0
    protected var encoderWidth: Int = 0
    protected var encoderHeight: Int = 0
    protected var screenShotCallback: ScreenShotCallback? = null
    protected var streamRotation = 0
    protected var muteVideo = false
    protected var isStreamHorizontalFlip = false
    protected var isStreamVerticalFlip = false
    protected var mForceRender = false

    constructor(context: Context):super(context)

    constructor(context: Context, attrs: AttributeSet):super(context, attrs)


    override fun setForceRender(force: Boolean) {
        this.mForceRender = force
    }

    override fun setIsStreamHorizontalFlip(flip: Boolean) {
        this.isStreamHorizontalFlip = flip
    }

    override fun setIsStreamVerticalFlip(flip: Boolean) {
        this.isStreamVerticalFlip = flip
    }

    override fun muteVideo() {
        muteVideo = true
    }

    override fun unMuteVideo() {
        muteVideo = false
    }

    override fun isVideoMuted(): Boolean {
        return muteVideo
    }

    override fun setFps(fps: Int) {
        fpsLimiter.setFps(fps)
    }

    override fun screenShot(screenShotCallback: ScreenShotCallback) {
        this.screenShotCallback = screenShotCallback
    }

    override fun addMediaCodecSurface(surface: Surface) {
        synchronized(sync) {
            if (surfaceManager.isReady()) {
                surfaceManagerPhoto.release()
                surfaceManagerEncoder.release()
                surfaceManagerEncoder.eglSetup(2, 2, surface, surfaceManager.getEglContext())
                surfaceManagerPhoto.eglSetup(encoderWidth, encoderHeight, null, surfaceManagerEncoder.getEglContext())
            }
        }
    }

    override fun removeMediaCodecSurface() {
        synchronized(sync) {
            surfaceManagerPhoto.release()
            surfaceManagerEncoder.release()
            surfaceManagerPhoto.eglSetup(encoderWidth, encoderHeight, null, surfaceManager.getEglContext())
        }
    }

    override fun setEncoderSize(width: Int, height: Int) {
        this.encoderWidth = width
        this.encoderHeight = height
    }

    override fun start() {
        synchronized(sync) {
            ADLogUtil.logD(TAG, "Thread started")
            glThread = Thread(this, "glThread")
            running = true
            glThread!!.start()
            semaphore.acquireUninterruptibly()
        }
    }

    override fun stop() {
        synchronized(sync) {
            if (glThread != null) {
                glThread!!.interrupt()
                try {
                    glThread!!.join(100)
                } catch (e: InterruptedException) {
                    glThread!!.interrupt()
                }
                glThread = null
            }
            surfaceManagerPhoto.release()
            surfaceManagerEncoder.release()
            surfaceManager.release()
            running = false
        }
    }

    override fun onFrameAvailable(surfaceTexture: SurfaceTexture?) {
        synchronized(sync) {
            frameAvailable = true
        }
    }

    abstract override fun getSurface(): Surface

    abstract override fun getSurfaceTexture(): SurfaceTexture

}