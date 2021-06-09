package com.magi.adlive.gl.filter

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import com.magi.adlive.R
import com.magi.adlive.util.createProgram
import com.magi.adlive.util.getStringFromRaw
import java.nio.ByteBuffer
import java.nio.ByteOrder

class NoFilterRender: BaseFilterRender() {

    //rotation matrix
    private val squareVertexDataFilter = floatArrayOf(
        // X, Y, Z, U, V
        -1f, -1f, 0f, 0f, 0f,  //bottom left
        1f, -1f, 0f, 1f, 0f,  //bottom right
        -1f, 1f, 0f, 0f, 1f,  //top left
        1f, 1f, 0f, 1f, 1f   //top right
    )

    private var program = -1
    private var aPositionHandle = -1
    private var aTextureHandle = -1
    private var uMVPMatrixHandle = -1
    private var uSTMatrixHandle = -1
    private var uSamplerHandle = -1

    init {
        squareVertex = ByteBuffer.allocateDirect(squareVertexDataFilter.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        squareVertex.put(squareVertexDataFilter).position(0)
        Matrix.setIdentityM(MVPMatrix, 0)
        Matrix.setIdentityM(STMatrix, 0)
    }

    override fun initGLFilter(context: Context) {
        val vertexShader = getStringFromRaw(R.raw.simple_vertex)
        val fragmentShader = getStringFromRaw(R.raw.simple_fragment)

        program = createProgram(vertexShader, fragmentShader)
        aPositionHandle = GLES20.glGetAttribLocation(program, "aPosition")
        aTextureHandle = GLES20.glGetAttribLocation(program, "aTextureCoord")
        uMVPMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        uSTMatrixHandle = GLES20.glGetUniformLocation(program, "uSTMatrix")
        uSamplerHandle = GLES20.glGetUniformLocation(program, "uSampler")
    }

    override fun drawFilter() {
        GLES20.glUseProgram(program)

        squareVertex.position(SQUARE_VERTEX_DATA_POS_OFFSET)
        GLES20.glVertexAttribPointer(
            aPositionHandle, 3, GLES20.GL_FLOAT, false,
            SQUARE_VERTEX_DATA_STRIDE_BYTES, squareVertex
        )
        GLES20.glEnableVertexAttribArray(aPositionHandle)

        squareVertex.position(SQUARE_VERTEX_DATA_UV_OFFSET)
        GLES20.glVertexAttribPointer(
            aTextureHandle, 2, GLES20.GL_FLOAT, false,
            SQUARE_VERTEX_DATA_STRIDE_BYTES, squareVertex
        )
        GLES20.glEnableVertexAttribArray(aTextureHandle)

        GLES20.glUniformMatrix4fv(uMVPMatrixHandle, 1, false, MVPMatrix, 0)
        GLES20.glUniformMatrix4fv(uSTMatrixHandle, 1, false, STMatrix, 0)

        GLES20.glUniform1i(uSamplerHandle, 4)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE4)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, getPreviousTexId())
    }

    override fun release() {
        GLES20.glDeleteProgram(program)
    }
}