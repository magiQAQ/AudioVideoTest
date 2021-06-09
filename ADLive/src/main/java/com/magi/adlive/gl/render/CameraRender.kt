package com.magi.adlive.gl.render

import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.opengl.Matrix
import android.view.Surface
import com.magi.adlive.R
import com.magi.adlive.gl.filter.BaseRenderOffScreen
import com.magi.adlive.util.*
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * 提供一个可供Camera输出的surface,并负责渲染
 */
class CameraRender: BaseRenderOffScreen() {
    private val textureID = IntArray(1)
    private val rotationMatrix = FloatArray(16)
    private val scaleMatrix = FloatArray(16)

    private var program = -1
    private var uMVPMatrixHandle = -1
    private var uSTMatrixHandle = -1
    private var aPositionHandle = -1
    private var aTextureCameraHandle = -1

    private lateinit var surfaceTexture: SurfaceTexture
    private lateinit var surface: Surface

    init {
        Matrix.setIdentityM(MVPMatrix, 0)
        Matrix.setIdentityM(STMatrix, 0)
        val vertex = getVerticesData()
        squareVertex = ByteBuffer.allocateDirect(vertex.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        squareVertex.put(vertex).position(0)
        setRotation(0)
        setFlip(false, false)
    }

    override fun initGL(width: Int, height: Int, context: Context, previewWidth: Int, previewHeight: Int) {
        this.width = width
        this.height = height
        checkGLError("initCameraGL start")
        val vertexShader = getStringFromRaw(R.raw.simple_vertex)
        val fragmentShader = getStringFromRaw(R.raw.camera_fragment)

        program = createProgram(vertexShader, fragmentShader)
        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        aTextureCameraHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
        uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        uSTMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")

        // camera texture
        createExternalTextures(textureID.size, textureID, 0)
        surfaceTexture = SurfaceTexture(textureID[0])
        surfaceTexture.setDefaultBufferSize(width, height)
        surface = Surface(surfaceTexture)
        initFBO(width, height)
        checkGLError("initCameraGL end")
    }

    override fun draw() {
        checkGLError("drawCamera start")
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, renderHandler.getFboId()[0])

        surfaceTexture.getTransformMatrix(STMatrix)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glUseProgram(program)
        GLES20.glClearColor(0.0f, 0.0f, 0.0f, 1.0f)
        GLES20.glClear(GLES20.GL_DEPTH_BUFFER_BIT or GLES20.GL_COLOR_BUFFER_BIT)
        squareVertex.position(SQUARE_VERTEX_DATA_POS_OFFSET)
        GLES20.glVertexAttribPointer(
            aPositionHandle, 3, GLES20.GL_FLOAT, false,
            SQUARE_VERTEX_DATA_STRIDE_BYTES, squareVertex
        )
        GLES20.glEnableVertexAttribArray(aPositionHandle)
        squareVertex.position(SQUARE_VERTEX_DATA_UV_OFFSET)
        GLES20.glVertexAttribPointer(
            aTextureCameraHandle, 2, GLES20.GL_FLOAT, false,
            SQUARE_VERTEX_DATA_STRIDE_BYTES, squareVertex
        )
        GLES20.glEnableVertexAttribArray(aTextureCameraHandle)
        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, MVPMatrix, 0)
        GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, STMatrix, 0)
        //camera
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, textureID[0])
        //draw
        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        checkGLError("drawCamera end")
    }

    override fun release() {
        GLES20.glDeleteProgram(program)
        surfaceTexture.release()
        surface.release()
    }

    fun updateTexImage() { surfaceTexture.updateTexImage() }

    fun getSurfaceTexture(): SurfaceTexture { return surfaceTexture }

    fun getSurface(): Surface { return surface }

    fun setRotation(rotation: Int) {
        Matrix.setIdentityM(rotationMatrix, 0)
        Matrix.rotateM(rotationMatrix, 0, rotation.toFloat(), 0f, 0f, -1f)
        update()
    }

    fun setFlip(isFlipHorizontal: Boolean, isFlipVertical: Boolean) {
        Matrix.setIdentityM(scaleMatrix, 0)
        Matrix.scaleM(scaleMatrix, 0, if (isFlipHorizontal) -1f else 1f, if (isFlipVertical) -1f else 1f, 1f)
        update()
    }

    private fun update() {
        Matrix.setIdentityM(MVPMatrix, 0)
        Matrix.multiplyMM(MVPMatrix, 0, scaleMatrix, 0, MVPMatrix, 0)
        Matrix.multiplyMM(MVPMatrix, 0, rotationMatrix, 0, MVPMatrix, 0)
    }
}