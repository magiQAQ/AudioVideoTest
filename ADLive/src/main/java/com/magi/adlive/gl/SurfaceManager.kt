package com.magi.adlive.gl

import android.opengl.*
import android.util.Log
import android.view.Surface
import com.magi.adlive.util.ADLogUtil
import com.magi.adlive.util.checkEGLError

class SurfaceManager {
    private val TAG = "SurfaceManager"
    private val EGL_RECORDABLE_ANDROID = 0x3142

    private var eglContext = EGL14.EGL_NO_CONTEXT
    private var eglSurface = EGL14.EGL_NO_SURFACE
    private var eglDisplay = EGL14.EGL_NO_DISPLAY
    @Volatile
    private var isReady = false

    fun isReady(): Boolean { return isReady }

    fun makeCurrent() {
        if (!EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)) {
            ADLogUtil.logE(TAG, "egl make current failed")
        }
    }

    fun swapBuffer() {
        if (!EGL14.eglSwapBuffers(eglDisplay, eglSurface)) {
            ADLogUtil.logE(TAG, "egl swap buffer failed")
        }
    }

    /**
     * 发送演示时间戳, 单位纳秒
     */
    fun setPresentationTime(nanos: Long) {
        EGLExt.eglPresentationTimeANDROID(eglDisplay, eglSurface, nanos)
        checkEGLError("egl presentation time Android")
    }

    /**
     * 初始化 egl
     */
    fun eglSetup(width: Int, height: Int, surface: Surface?, eglSharedContext: EGLContext?) {
        if (isReady) {
            ADLogUtil.logE(TAG, "already ready, ignored")
            return
        }
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (eglDisplay == EGL14.EGL_NO_DISPLAY) {
            ADLogUtil.logE(TAG, "unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(eglDisplay, version, 0, version, 1)) {
            ADLogUtil.logE(TAG, "unable to initialize EGL14")
        }

        val attribList: IntArray = when {
            eglSharedContext == null && surface == null -> intArrayOf(
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
            )
            eglSharedContext == null -> intArrayOf(
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
                EGL14.EGL_NONE
            )
            surface == null -> intArrayOf(
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
            )
            else -> intArrayOf(
                EGL14.EGL_RED_SIZE, 8, EGL14.EGL_GREEN_SIZE, 8, EGL14.EGL_BLUE_SIZE, 8,
                EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT, EGL_RECORDABLE_ANDROID, 1,
                EGL14.EGL_NONE
            )
        }
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, attribList, 0, configs, 0, configs.size, numConfigs, 0)
        checkEGLError("egl create context, RGB8888 + recordable ES2")

        val attrib_list = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(
            eglDisplay, configs[0], eglSharedContext?:EGL14.EGL_NO_CONTEXT, attrib_list, 0
        )
        checkEGLError("egl create context")

        if (surface == null) {
            val surfaceAttribs = intArrayOf(EGL14.EGL_WIDTH, width, EGL14.EGL_HEIGHT, height, EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, configs[0], surfaceAttribs, 0)
        } else {
            val surfaceAttribs = intArrayOf(EGL14.EGL_NONE)
            eglSurface = EGL14.eglCreateWindowSurface(eglDisplay, configs[0], surface, surfaceAttribs, 0)
        }
        checkEGLError("egl create windows surface")
        isReady = true
        ADLogUtil.logD(TAG, "GL initialized")
    }

//    fun eglSetup(surface: Surface?, manager: SurfaceManager) { eglSetup(2, 2, surface, manager.eglContext) }
//
//    fun eglSetup(width: Int, height: Int, manager: SurfaceManager) { eglSetup(width, height, null, manager.eglContext) }
//
//    fun eglSetup(surface: Surface?, eglContext: EGLContext?) { eglSetup(2, 2, surface, eglContext) }
//
//    fun eglSetup(surface: Surface?) { eglSetup(2, 2, surface, null) }
//
//    fun eglSetup() { eglSetup(2, 2, null, null) }

    /**
     *  释放当前对象所有的资源, 特别是EglContext
     */
    fun release() {
        if (eglDisplay != EGL14.EGL_NO_DISPLAY) {
            EGL14.eglMakeCurrent(eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
            EGL14.eglDestroySurface(eglDisplay, eglSurface)
            EGL14.eglDestroyContext(eglDisplay, eglContext)
            EGL14.eglReleaseThread()
            EGL14.eglTerminate(eglDisplay)
            ADLogUtil.logD(TAG, "GL released")
            eglDisplay = EGL14.EGL_NO_DISPLAY
            eglContext = EGL14.EGL_NO_CONTEXT
            eglSurface = EGL14.EGL_NO_SURFACE
            isReady = false
        } else {
            ADLogUtil.logE(TAG, "GL already released")
        }
    }

    fun getEglContext():EGLContext { return eglContext }
    fun getEglSurface():EGLSurface { return eglSurface }
    fun getEglDisplay():EGLDisplay { return eglDisplay }
}