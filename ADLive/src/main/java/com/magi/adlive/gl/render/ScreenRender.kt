package com.magi.adlive.gl.render

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import com.magi.adlive.R
import com.magi.adlive.gl.filter.BaseRenderOffScreen
import com.magi.adlive.gl.filter.BaseRenderOffScreen.Companion.FLOAT_SIZE_BYTES
import com.magi.adlive.gl.filter.BaseRenderOffScreen.Companion.SQUARE_VERTEX_DATA_POS_OFFSET
import com.magi.adlive.gl.filter.BaseRenderOffScreen.Companion.SQUARE_VERTEX_DATA_STRIDE_BYTES
import com.magi.adlive.gl.filter.BaseRenderOffScreen.Companion.SQUARE_VERTEX_DATA_UV_OFFSET
import com.magi.adlive.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer

/**
 * 负责在用户的预览surface上进行渲染
 */
class ScreenRender {

    private val squareVertexData = floatArrayOf(
        // X, Y, Z, U, V
        -1f, -1f, 0f, 0f, 0f, //bottom left
        1f, -1f, 0f, 1f, 0f, //bottom right
        -1f, 1f, 0f, 0f, 1f, //top left
        1f, 1f, 0f, 1f, 1f //top right
    )

    private var squareVertex: FloatBuffer

    private val mvpMatrix = FloatArray(16)
    private val stMatrix = FloatArray(16)
    private var aaEnabled = false

    private var texId = -1
    private var program = -1
    private var uMVPMatrixHandle = -1
    private var uSTMatrixHandle = -1
    private var aPositionHandle = -1
    private var aTextureHandle = -1
    private var uSamplerHandle = -1
    private var uResolutionHandle = -1
    private var uAAEnabledHandle = -1

    private var streamWidth = 0
    private var streamHeight = 0
    private var isPortrait = false

    init {
        squareVertex = ByteBuffer.allocateDirect(squareVertexData.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        squareVertex.put(squareVertexData).position(0)
        Matrix.setIdentityM(mvpMatrix, 0)
        Matrix.setIdentityM(stMatrix, 0)
    }

    fun initGL(context: Context) {
        isPortrait = isPortrait(context)
        checkGLError("initScreenGL start")
        val vertexShader = getStringFromRaw(R.raw.simple_vertex)
        val fragmentShader = getStringFromRaw(R.raw.fxaa)

        program = createProgram(vertexShader, fragmentShader)
        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        aTextureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
        uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        uSTMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")
        uSamplerHandle = GLES20.glGetUniformLocation(program, "uSampler")
        uResolutionHandle = GLES20.glGetUniformLocation(program, "uResolution")
        uAAEnabledHandle = GLES20.glGetUniformLocation(program, "uAAEnabled")

        checkGLError("initScreenGL end")
    }

    fun draw(width: Int, height: Int, keepAspectRatio: Boolean, mode: Int, rotation: Int,
        isPreview: Boolean, flipStreamVertical: Boolean, flipStreamHorizontal: Boolean) {
        checkGLError("drawScreen start")

        processMatrix(rotation, width, height, isPreview, isPortrait, flipStreamHorizontal,
            flipStreamVertical, mode, mvpMatrix)
        calculateViewPort(keepAspectRatio, mode, width, height, streamWidth, streamHeight)

        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)

        GLES20.glUseProgram(program)

        squareVertex.position(SQUARE_VERTEX_DATA_POS_OFFSET)
        GLES20.glVertexAttribPointer(
            aPositionHandle, 3, GLES20.GL_FLOAT, false,
            BaseRenderOffScreen.SQUARE_VERTEX_DATA_STRIDE_BYTES, squareVertex
        )
        GLES20.glEnableVertexAttribArray(aPositionHandle)

        squareVertex.position(SQUARE_VERTEX_DATA_UV_OFFSET)
        GLES20.glVertexAttribPointer(
            aTextureHandle, 2, GLES20.GL_FLOAT, false,
            SQUARE_VERTEX_DATA_STRIDE_BYTES, squareVertex
        )
        GLES20.glEnableVertexAttribArray(aTextureHandle)

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, stMatrix, 0)
        GLES20.glUniform2f(uResolutionHandle, width.toFloat(), height.toFloat())
        GLES20.glUniform1f(uAAEnabledHandle, if (aaEnabled) 1f else 0f)

        GLES20.glUniform1i(uSamplerHandle, 5)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE5)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        //draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        checkGLError("drawScreen end")
    }

    fun release() {
        GLES20.glDeleteProgram(program)
    }

    fun setTexId(texId: Int) { this.texId = texId }

    fun setAAEnabled(aaEnabled: Boolean) { this.aaEnabled = aaEnabled }

    fun isAAEnable(): Boolean { return aaEnabled }

    fun setStreamSize(streamWidth: Int, streamHeight: Int) {
        this.streamHeight = streamHeight
        this.streamWidth = streamWidth
    }
}