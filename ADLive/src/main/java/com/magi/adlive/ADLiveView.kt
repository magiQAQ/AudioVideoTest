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
    private var AAEnabled = false
    private var keepAspectRatio = false
    private var aspectRatioMode: AspectRatioMode = AspectRatioMode.Adjust
    private var screenSurfaceTexture: SurfaceTexture? = null
    private var screenSurface: Surface? = null
    //todo 信号量
    private var needPreview: Semaphore = Semaphore(1)


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
        this.AAEnabled = enable
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

    override fun run() {
//        if (!needPreview) return
        surfaceManager.release()
        surfaceManager.eglSetup(2, 2, screenSurface!! ,null)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        screenSurfaceTexture = surface
        screenSurface = Surface(surface)
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        this.previewWidth = width
        this.previewHeight = height
        managerRender.setPreviewSize(previewWidth, previewHeight)
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        return true
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}
}