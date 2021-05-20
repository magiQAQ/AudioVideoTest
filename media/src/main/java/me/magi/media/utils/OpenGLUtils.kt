package me.magi.media.utils

import android.opengl.GLES11Ext
import android.opengl.GLES20
import android.util.Log
import javax.microedition.khronos.opengles.GL
import javax.microedition.khronos.opengles.GL10

private const val TAG = "OpenGLUtils"

fun getExternalOESTextureID(): Int {
    val texture = intArrayOf(1)
    GLES20.glGenTextures(1, texture, 0)
    GLES20.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES,texture[0])
    GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR.toFloat())
    GLES20.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat())
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE)
    GLES20.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE)
    return texture[0]
}

fun loadShader(type: Int, source: String): Int {
    // 1.create shader
    var shader = GLES20.glCreateShader(type)
    if (shader == GLES20.GL_NONE) {
        Log.e(TAG, "create shader failed! type: $type")
        return GLES20.GL_NONE
    }
    // 2.load shader source
    GLES20.glShaderSource(shader, source)
    // 3.compile shared source
    GLES20.glCompileShader(shader)
    // 4.check compile status
    val compiled = IntArray(1)
    GLES20.glGetShaderiv(shader, GLES20.GL_COMPILE_STATUS, compiled, 0)
    if (compiled[0] == GLES20.GL_FALSE) { // compile failed
        Log.e(TAG, "Error compiling shader. type: $type:")
        Log.e(TAG, GLES20.glGetShaderInfoLog(shader))
        GLES20.glDeleteShader(shader)
        shader = GLES20.GL_NONE
    }
    return shader
}

fun createProgram(vertexSource: String, fragmentSource: String): Int {
    // 1.load shader
    val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexSource)
    if (vertexShader == GLES20.GL_NONE) {
        Log.e(TAG, "load vertex shader failed! ")
        return GLES20.GL_NONE
    }
    val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentSource)
    if (fragmentShader == GLES20.GL_NONE) {
        Log.e(TAG, "load fragment shader failed! ")
        return GLES20.GL_NONE
    }
    // 2.create gl program
    val program = GLES20.glCreateProgram()
    if (program == GLES20.GL_NONE) {
        Log.e(TAG, "create program failed! ")
        return GLES20.GL_NONE
    }
    // 3. attach shader
    GLES20.glAttachShader(program, vertexShader)
    GLES20.glAttachShader(program, fragmentShader)
    // we can delete shader after attach
    GLES20.glDeleteShader(vertexShader)
    GLES20.glDeleteShader(fragmentShader)
    // 4. link program
    GLES20.glLinkProgram(program)
    // 5. check link status
    val linkStatus = IntArray(1)
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, linkStatus, 0)
    if (linkStatus[0] == GLES20.GL_FALSE) { // link failed
        Log.e(TAG, "Error link program: ")
        Log.e(TAG, GLES20.glGetProgramInfoLog(program))
        GLES20.glDeleteProgram(program); // delete program
        return GLES20.GL_NONE
    }
    return program
}