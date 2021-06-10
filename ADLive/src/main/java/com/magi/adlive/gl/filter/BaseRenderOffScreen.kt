package com.magi.adlive.gl.filter

import android.content.Context
import android.opengl.GLES20
import com.magi.adlive.gl.RenderHandler
import com.magi.adlive.util.ADLogUtil
import com.magi.adlive.util.checkGLError
import java.nio.FloatBuffer

abstract class BaseRenderOffScreen {

    companion object{
        internal const val FLOAT_SIZE_BYTES = 4
        internal const val SQUARE_VERTEX_DATA_STRIDE_BYTES = FLOAT_SIZE_BYTES * 5
        internal const val SQUARE_VERTEX_DATA_POS_OFFSET = 0
        internal const val SQUARE_VERTEX_DATA_UV_OFFSET = 3
        private const val TAG = "BaseRenderOffScreen"
    }

    protected lateinit var squareVertex: FloatBuffer
    protected val MVPMatrix = FloatArray(16)
    protected val STMatrix = FloatArray(16)
    protected var renderHandler = RenderHandler()
    protected var width = 0
    protected var height = 0

    abstract fun initGL(width: Int, height: Int, context: Context, previewWidth: Int, previewHeight: Int)

    abstract fun draw()

    abstract fun release()

    fun getTexId(): Int { return renderHandler.getTexId()[0] }

    protected fun initFBO(width: Int, height: Int) {
        initFBO(width, height, renderHandler.getFboId(), renderHandler.getRboId(), renderHandler.getTexId())
    }

    protected fun initFBO(width: Int, height: Int, fboId: IntArray, rboId: IntArray, texId: IntArray) {
        checkGLError("initFBO_S")

        GLES20.glGenFramebuffers(1, fboId, 0)
        GLES20.glGenRenderbuffers(1, rboId, 0)
        GLES20.glGenTextures(1, texId, 0)

        GLES20.glBindRenderbuffer(GLES20.GL_RENDERBUFFER, rboId[0])
        GLES20.glRenderbufferStorage(GLES20.GL_RENDERBUFFER, GLES20.GL_DEPTH_COMPONENT16, width, height)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fboId[0])
        GLES20.glFramebufferRenderbuffer(GLES20.GL_FRAMEBUFFER, GLES20.GL_DEPTH_ATTACHMENT, GLES20.GL_RENDERBUFFER, rboId[0])

        GLES20.glActiveTexture(GLES20.GL_TEXTURE3)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
        GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, texId[0], 0)

        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            ADLogUtil.logE(TAG, "FrameBuffer uncompleted code: $status")
        }

        checkGLError("initFBO_E")
    }
}