package com.magi.adlive

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.view.Surface
import com.magi.adlive.gl.Filter
import com.magi.adlive.gl.render.ManagerRender
import com.magi.adlive.gl.filter.BaseFilterRender
import com.magi.adlive.model.AspectRatioMode
import com.magi.adlive.widget.ADLiveGLViewBase
import java.util.concurrent.Semaphore

class ADLiveView: ADLiveGLViewBase {

    private lateinit var managerRender: ManagerRender
    private var loadAA = false
    private var aaEnabled = false
    private var keepAspectRatio = false
    private var aspectRatioMode: AspectRatioMode = AspectRatioMode.Adjust
    private var screenSurfaceTexture: SurfaceTexture? = null
    private var screenSurface: Surface? = null

    private var needPreview: Semaphore = Semaphore(-1)


    constructor(context: Context): super(context)

    constructor(context: Context, attrs: AttributeSet): super(context, attrs)

    init {
        surfaceTextureListener = this
    }

    override fun init() {
        if (!initialized) managerRender = ManagerRender(2)
        initialized = true
    }

    override fun getSurface(): Surface {
        return managerRender.getSurface()
    }

    override fun getSurfaceTexture(): SurfaceTexture {
        return managerRender.getSurfaceTexture()
    }

    override fun setFilter(filterPosition: Int, baseFilterRender: BaseFilterRender) {
        filterQueue.add(Filter(filterPosition, baseFilterRender))
    }

    override fun setFilter(baseFilterRender: BaseFilterRender) {
        setFilter(0, baseFilterRender)
    }

    override fun enableAA(enable: Boolean) {
        this.aaEnabled = enable
        loadAA = true
    }

    override fun setRotation(rotation: Int) {
        managerRender.setCameraRotation(rotation)
    }

    fun setAspectRatioMode(aspectRatioMode: AspectRatioMode) {
        this.aspectRatioMode = aspectRatioMode
    }

    fun setKeepAspectRatio(keepAspectRatio: Boolean) {
        this.keepAspectRatio = keepAspectRatio
    }

    fun isKeepAspectRatio(): Boolean {
        return keepAspectRatio
    }

    override fun isAAEnabled(): Boolean {
        return managerRender.isAaEnabled()
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        screenSurfaceTexture = surface
        screenSurfaceTexture?.setDefaultBufferSize(width, height)
        needPreview.release()
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        this.previewWidth = width
        this.previewHeight = height
        managerRender.setPreviewSize(previewWidth, previewHeight)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        screenSurface?.release()
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    override fun run() {
        needPreview.acquireUninterruptibly()
        surfaceManager.release()
        surfaceManager.eglSetup(2, 2, screenSurface!! ,null)
        surfaceManager.makeCurrent()
        managerRender.initGL(context, encoderWidth, encoderHeight, previewWidth, previewHeight)
        managerRender.getSurfaceTexture().setOnFrameAvailableListener(this)
        surfaceManagerPhoto.release()
        surfaceManagerPhoto.eglSetup(encoderWidth, encoderHeight, null, surfaceManager.getEglContext())
        semaphore.release()
        try {
            while (running) {
                if (frameAvailable || mForceRender) {
                    frameAvailable = false
                    surfaceManager.makeCurrent()
                    managerRender.updateFrame()
                    managerRender.drawOffScreen()
                    managerRender.drawScreen(previewWidth, previewHeight, keepAspectRatio, aspectRatioMode.id, 0,
                        isPreview = true, flipStreamVertical = false, flipStreamHorizontal = false
                    )
                    surfaceManager.swapBuffer()
                }

                synchronized(sync) {
                    if (surfaceManagerEncoder.isReady() && !fpsLimiter.limitFPS()) {
                        val w = if (muteVideo) 0 else encoderWidth
                        val h = if (muteVideo) 0 else encoderHeight
                        surfaceManagerEncoder.makeCurrent()
                        managerRender.drawScreen(w, h, false, aspectRatioMode.id,
                        streamRotation, false, isStreamVerticalFlip, isStreamHorizontalFlip)
                        surfaceManagerEncoder.swapBuffer()
                    }
                    if (screenShotCallback != null && surfaceManagerPhoto.isReady()) {
                        surfaceManagerPhoto.makeCurrent()
                        managerRender.drawScreen(encoderWidth, encoderHeight, false, aspectRatioMode.id,
                        streamRotation, false, isStreamVerticalFlip, isStreamHorizontalFlip)
                        screenShotCallback?.onScreenShot(getBitmap(encoderWidth, encoderHeight))
                        screenShotCallback = null
                        surfaceManagerPhoto.swapBuffer()
                    }
                }
                if (!filterQueue.isEmpty()) {
                    val filter = filterQueue.take()
                    managerRender.setFilter(filter.position, filter.baseFilterRender)
                } else if (loadAA) {
                    managerRender.enableAA(aaEnabled)
                    loadAA = false
                }
            }
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } finally {
            managerRender.release()
            surfaceManager.release()
            surfaceManagerPhoto.release()
            surfaceManagerEncoder.release()
        }
    }
}