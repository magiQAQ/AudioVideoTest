package com.magi.adlive.gl.filter

import android.content.Context
import android.opengl.GLES20
import com.magi.adlive.util.checkGLError

abstract class BaseFilterRender: BaseRenderOffScreen(){

    var previewWidth = 0
        private set
    var previewHeight = 0
        private set

    @JvmField
    protected var previousTexId = 0

    override fun initGL(width: Int, height: Int, context: Context, previewWidth: Int, previewHeight: Int) {
        this.width = width
        this.height = height
        this.previewWidth = previewWidth
        this.previewHeight = previewHeight
        checkGLError("initGl start")
        initGLFilter(context)
        checkGLError("initGl end")
    }

    fun setPreviewSize(previewWidth: Int, previewHeight: Int) {
        this.previewWidth = previewWidth
        this.previewHeight = previewHeight
    }

    fun initFBOLink() {
        initFBO(width, height, renderHandler.getFboId(), renderHandler.getRboId(), renderHandler.getTexId())
    }

    override fun draw() {
        checkGLError("drawFilter start")
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, renderHandler.getFboId()[0])
        GLES20.glViewport(0, 0, width, height)
        drawFilter()
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        checkGLError("drawFilter end")
    }

    protected abstract fun initGLFilter(context: Context)

    protected abstract fun drawFilter()

    fun setPreviousTexId(texId: Int) { previousTexId = texId }

}