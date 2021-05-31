package me.magi.media.model

import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface

class MediaCodecGLWrapper {
    lateinit var eglDisplay: EGLDisplay
    lateinit var eglConfig: EGLConfig
    lateinit var eglSurface: EGLSurface
    lateinit var eglContext: EGLContext

    var drawProgram: Int = 0
    var drawTextureLoc: Int = 0
    var drawPositionLoc: Int = 0
    var drawTextureCoordLoc: Int = 0
}