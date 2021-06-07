package com.magi.adlive.util

import android.opengl.EGL14
import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import com.magi.adlive.ADLiveBase
import java.io.ByteArrayOutputStream
import java.io.IOException

private const val TAG = "ADGlUtil"

internal fun createProgram(vertexSource: String, fragmentSource: String): Int {
    val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
    if (vertexShader == 0) { return 0 }
    val pixelShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
    if (pixelShader == 0) { return 0 }
    var program = GLES20.glCreateProgram()
    checkGLError("glCreateProgram")
    if (program == 0) { ADLogUtil.logE(TAG, "Could not create program") }
    GLES20.glAttachShader(program, vertexShader)
    checkGLError("glAttachShader")
    GLES20.glAttachShader(program, pixelShader)
    checkGLError("glAttachShader")
    GLES20.glLinkProgram(program)
    val linkStatus = IntArray(1)
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
    if (linkStatus[0] != GLES20.GL_TRUE) {
        Log.e(TAG, "Could not link program: ")
        Log.e(TAG, GLES20.glGetProgramInfoLog(program))
        GLES20.glDeleteProgram(program)
        program = 0
    }
    return program
}

private fun loadShader(shaderType: Int, source: String): Int {
    var shader = GLES20.glCreateShader(shaderType)
    checkGLError("glCreateShader type=$shaderType")
    GLES20.glShaderSource(shader, source)
    GLES20.glCompileShader(shader)
    val compiled = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
    if (compiled[0] == 0) {
        ADLogUtil.logE(TAG, "Could not compile shader $shaderType:")
        ADLogUtil.logE(TAG, GLES20.glGetShaderInfoLog(shader)?:"")
        GLES20.glDeleteShader(shader)
        shader = 0
    }
    return shader
}

internal fun createTextures(quantity: Int, texturesId: IntArray, offset: Int) {
    GLES20.glGenTextures(quantity, texturesId, offset)
    for (i in offset until quantity) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texturesId[i])
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }
}

internal fun createExternalTextures(quantity: Int, texturesId: IntArray, offset: Int) {
    GLES20.glGenTextures(quantity, texturesId, offset)
    for (i in offset until quantity) {
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0 + i)
        GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, texturesId[i])
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
    }
}

internal fun checkGLError(op: String) {
    val error = GLES20.glGetError()
    if (error != GLES20.GL_NO_ERROR) {
        ADLogUtil.logE("GLError", "$op. GL Error: $error")
    }
}

internal fun checkEGLError(op: String) {
    val error = EGL14.eglGetError()
    if (error != EGL14.EGL_SUCCESS) {
        ADLogUtil.logE("GLError", "$op. GL Error: $error")
    }
}

internal fun getStringFromRaw(rawId: Int): String {
    var result: String
    try {
        val res = ADLiveBase.application.resources
        val inputStream = res.openRawResource(rawId)
        val outputStream = ByteArrayOutputStream()
        var i = inputStream.read()
        while (i != -1) {
            outputStream.write(i)
            i = inputStream.read()
        }
        result = outputStream.toString()
        inputStream.close()
        outputStream.close()
    } catch (e: IOException) {
        e.printStackTrace()
        ADLogUtil.logE(message = "getStringFromRaw ${e.message}")
        result = ""
    }
    return result
}

