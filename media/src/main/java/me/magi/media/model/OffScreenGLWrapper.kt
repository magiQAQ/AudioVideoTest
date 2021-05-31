package me.magi.media.model

import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface

class OffScreenGLWrapper {
    lateinit var eglDisplay: EGLDisplay
    lateinit var eglConfig: EGLConfig
    lateinit var eglSurface: EGLSurface
    lateinit var eglContext: EGLContext

    var camera2dProgram = 0
    var camera2dTextureMatrix = 0
    var camera2dTextureLoc = 0
    var camera2dPositionLoc = 0
    var camera2dTextureCoordLoc = 0

    var cameraProgram = 0
    var cameraTextureLoc = 0
    var cameraPositionLoc = 0
    var cameraTextureCoordLoc = 0
}