package com.magi.adlive.gl.render

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Surface
import com.magi.adlive.gl.filter.BaseFilterRender
import com.magi.adlive.gl.filter.NoFilterRender

class ManagerRender(val numFilters: Int) {


    private val cameraRender = CameraRender()
    private val baseFilterRenders = ArrayList<BaseFilterRender>(numFilters)
    private val screenRender = ScreenRender()

    private var width = 0
    private var height = 0
    private var previewWidth = 0
    private var previewHeight = 0
    private var context: Context? = null

    init {
        for (i in 0 until numFilters) baseFilterRenders.add(NoFilterRender())
    }

    fun initGL(
        context: Context, encoderWidth: Int, encoderHeight: Int,
        previewWidth: Int, previewHeight: Int
    ) {
        this.context = context
        this.width = encoderWidth
        this.height = encoderHeight
        this.previewWidth = previewWidth
        this.previewHeight = previewHeight
        cameraRender.initGL(width, height, context, previewWidth, previewHeight)
        for (i in 0 until numFilters) {
            val texId = if (i == 0) cameraRender.getTexId() else baseFilterRenders[i - 1].getTexId()
            baseFilterRenders[i].setPreviousTexId(texId)
            baseFilterRenders[i].initGL(width, height, context, previewWidth, previewHeight)
            baseFilterRenders[i].initFBOLink()
        }
        screenRender.setStreamSize(encoderWidth, encoderHeight)
        screenRender.setTexId(baseFilterRenders[numFilters - 1].getTexId())
        screenRender.initGL(context)
    }

    fun drawOffScreen() {
        cameraRender.draw()
        for (render in baseFilterRenders) render.draw()
    }

    fun drawScreen(
        width: Int, height: Int, keepAspectRatio: Boolean, mode: Int,
        rotation: Int, isPreview: Boolean, flipStreamVertical: Boolean,
        flipStreamHorizontal: Boolean
    ) {
        screenRender.draw(width, height, keepAspectRatio, mode, rotation, isPreview, flipStreamVertical, flipStreamHorizontal)
    }

    fun release() {
        cameraRender.release()
        baseFilterRenders.forEach {
            it.release()
        }
        screenRender.release()
    }

    fun enableAA(aaEnable: Boolean) {
        screenRender.setAAEnabled(aaEnable)
    }

    fun isAaEnabled(): Boolean {
        return screenRender.isAAEnable()
    }

    fun updateFrame() {
        cameraRender.updateTexImage()
    }

    fun getSurfaceTexture(): SurfaceTexture {
        return cameraRender.getSurfaceTexture()
    }

    fun getSurface(): Surface {
        return cameraRender.getSurface()
    }

    fun setCameraRotation(rotation: Int) {
        cameraRender.setRotation(rotation)
    }

    fun setCameraFlip(isFlipHorizontal: Boolean, isFlipVertical: Boolean) {
        cameraRender.setFlip(isFlipHorizontal, isFlipVertical)
    }

    fun setPreviewSize(previewWidth: Int, previewHeight: Int) {
        baseFilterRenders.forEach {
            it.setPreviewSize(previewWidth, previewHeight)
        }
    }
}