package me.magi.media.utils

import android.graphics.SurfaceTexture
import android.opengl.*
import android.view.Surface
import me.magi.media.model.MediaCodecGLWrapper
import me.magi.media.model.OffScreenGLWrapper
import me.magi.media.model.ScreenGLWrapper
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGL10

private const val EGL_RECORDABLE_ANDROID = 0x3142
private const val VERTEXSHADER =
    "attribute vec4 aPosition;\n" +
    "attribute vec2 aTextureCoord;\n" +
    "varying vec2 vTextureCoord;\n" +
    "void main(){\n" +
    "    gl_Position= aPosition;\n" +
    "    vTextureCoord = aTextureCoord;\n" +
    "}"
private const val VERTEXSHADER_CAMERA2D =
    "attribute vec4 aPosition;\n" +
    "attribute vec4 aTextureCoord;\n" +
    "uniform mat4 uTextureMatrix;\n" +
    "varying vec2 vTextureCoord;\n" +
    "void main(){\n" +
    "    gl_Position= aPosition;\n" +
    "    vTextureCoord = (uTextureMatrix * aTextureCoord).xy;\n" +
    "}"
private const val FRAGMENTSHADER_CAMERA =
    "#extension GL_OES_EGL_image_external : require\n" +
    "precision highp float;\n" +
    "varying highp vec2 vTextureCoord;\n" +
    "uniform sampler2D uTexture;\n" +
    "void main(){\n" +
    "    vec4  color = texture2D(uTexture, vTextureCoord);\n" +
    "    gl_FragColor = color;\n" +
    "}"
private const val FRAGMENTSHADER_CAMERA2D =
    "#extension GL_OES_EGL_image_external : require\n" +
    "precision highp float;\n" +
    "varying highp vec2 vTextureCoord;\n" +
    "uniform samplerExternalOES uTexture;\n" +
    "void main(){\n" +
    "    vec4  color = texture2D(uTexture, vTextureCoord);\n" +
    "    gl_FragColor = color;\n" +
    "}"
private const val FRAGMENTSHADER_2D =
    "precision highp float;\n" +
    "varying highp vec2 vTextureCoord;\n" +
    "uniform sampler2D uTexture;\n" +
    "void main(){\n" +
    "    vec4  color = texture2D(uTexture, vTextureCoord);\n" +
    "    gl_FragColor = color;\n" +
    "}"
private val drawIndices = shortArrayOf(0, 1, 2, 0, 2, 3)
private val SquareVertices = floatArrayOf(
    -1.0f, 1.0f,
    -1.0f, -1.0f,
    1.0f, -1.0f,
    1.0f, 1.0f
)
private val CamTextureVertices = floatArrayOf(
    0.0f, 1.0f,
    0.0f, 0.0f,
    1.0f, 0.0f,
    1.0f, 1.0f
)
private val Cam2dTextureVertices = floatArrayOf(
    0.0f, 1.0f,
    0.0f, 0.0f,
    1.0f, 0.0f,
    1.0f, 1.0f
)
private val Cam2dTextureVertices_90 = floatArrayOf(
    0.0f, 0.0f,
    1.0f, 0.0f,
    1.0f, 1.0f,
    0.0f, 1.0f
)
private val Cam2dTextureVertices_180 = floatArrayOf(
    1.0f, 0.0f,
    1.0f, 1.0f,
    0.0f, 1.0f,
    0.0f, 0.0f
)
private val Cam2dTextureVertices_270 = floatArrayOf(
    1.0f, 1.0f,
    0.0f, 1.0f,
    0.0f, 0.0f,
    1.0f, 0.0f
)
private val MediaCodecTextureVertices = floatArrayOf(
    0.0f, 1.0f,
    0.0f, 0.0f,
    1.0f, 0.0f,
    1.0f, 1.0f
)
private val ScreenTextureVertices = floatArrayOf(
    0.0f, 1.0f,
    0.0f, 0.0f,
    1.0f, 0.0f,
    1.0f, 1.0f
)
private const val FLOAT_SIZE_BYTES = 4
private const val SHORT_SIZE_BYTES = 2
private const val COORDS_PER_VERTEX = 2
private const val TEXTURE_COORDS_PER_VERTEX = 2

internal fun initOffScreenGL(wrapper: OffScreenGLWrapper) {
    wrapper.eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    if (EGL14.EGL_NO_SURFACE == wrapper.eglDisplay) {
        throw RuntimeException("eglGetDisplay,failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
    val versions = IntArray(2)
    if (!EGL14.eglInitialize(wrapper.eglDisplay, versions, 0, versions, 1)) {
        throw RuntimeException("eglInitialize,failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
    val configCount = IntArray(1)
    val configs = arrayOfNulls<EGLConfig>(1)
    val configSpec = intArrayOf(
        EGL14.EGL_RENDERABLE_TYPE,
        EGL14.EGL_OPENGL_ES2_BIT,
        EGL14.EGL_RED_SIZE, 8,
        EGL14.EGL_GREEN_SIZE, 8,
        EGL14.EGL_BLUE_SIZE, 8,
        EGL14.EGL_DEPTH_SIZE, 0,
        EGL14.EGL_STENCIL_SIZE, 0,
        EGL14.EGL_NONE
    )
    EGL14.eglChooseConfig(wrapper.eglDisplay, configSpec, 0, configs, 0, 1, configCount, 0)
    if (configCount[0] <= 0) {
        throw RuntimeException("eglChooseConfig,failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
    val eglConfig = configs[0] ?: throw RuntimeException("eglChooseConfig,failed: eglConfig is null")
    wrapper.eglConfig = eglConfig
    val contextSpec = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
    wrapper.eglContext = EGL14.eglCreateContext(
        wrapper.eglDisplay, wrapper.eglConfig, EGL14.EGL_NO_CONTEXT, contextSpec, 0
    )
    if (wrapper.eglContext == EGL14.EGL_NO_CONTEXT) {
        throw RuntimeException("egl create context failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
    val value = IntArray(1)
    EGL14.eglQueryContext(
        wrapper.eglDisplay, wrapper.eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION,
        value, 0
    )
    val surfaceAttributes = intArrayOf(
        EGL10.EGL_WIDTH, 1,
        EGL10.EGL_HEIGHT, 1,
        EGL14.EGL_NONE
    )
    wrapper.eglSurface = EGL14.eglCreatePbufferSurface(wrapper.eglDisplay, wrapper.eglConfig, surfaceAttributes, 0)
    if (wrapper.eglSurface == EGL14.EGL_NO_SURFACE) {
        throw RuntimeException("egl create window surface, failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
}

internal fun initMediaCodecGL(wrapper: MediaCodecGLWrapper, sharedContext: EGLContext, mediaInputSurface: Surface) {
    wrapper.eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    if (wrapper.eglDisplay == EGL14.EGL_NO_DISPLAY) {
        throw RuntimeException("eglGetDisplay failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
    val versions = IntArray(2)
    if (!EGL14.eglInitialize(wrapper.eglDisplay, versions, 0, versions, 1)) {
        throw RuntimeException("eglInitialize failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
    val configsCount = IntArray(1)
    val configs = arrayOfNulls<EGLConfig>(1)
    val configSpec = intArrayOf(
        EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
        EGL14.EGL_RED_SIZE, 8,
        EGL14.EGL_GREEN_SIZE, 8,
        EGL14.EGL_BLUE_SIZE, 8,
        EGL_RECORDABLE_ANDROID, 1,
        EGL14.EGL_DEPTH_SIZE, 0,
        EGL14.EGL_STENCIL_SIZE, 0,
        EGL14.EGL_NONE
    )
    EGL14.eglChooseConfig(wrapper.eglDisplay, configSpec, 0, configs, 0, 1, configsCount, 0)
    if (configsCount[0] <= 0) {
        throw RuntimeException("eglChooseConfig, failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
    val eglConfig = configs[0]?:throw RuntimeException("eglConfig is null")
    wrapper.eglConfig = eglConfig
    val contextSpec = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
    wrapper.eglContext = EGL14.eglCreateContext(wrapper.eglDisplay, wrapper.eglConfig, sharedContext, contextSpec, 0)
    if (wrapper.eglContext == EGL14.EGL_NO_CONTEXT) {
        throw RuntimeException("eglCreateContext failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
    val value = IntArray(0)
    EGL14.eglQueryContext(wrapper.eglDisplay, wrapper.eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, value, 0)
    val surfaceAttr = intArrayOf(EGL14.EGL_NONE)
    wrapper.eglSurface = EGL14.eglCreateWindowSurface(wrapper.eglDisplay, wrapper.eglConfig, mediaInputSurface, surfaceAttr, 0)
    if (wrapper.eglSurface == EGL14.EGL_NO_SURFACE) {
        throw RuntimeException("eglCreateWindowSurface failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
}

internal fun initScreenGL(wrapper: ScreenGLWrapper, sharedContext: EGLContext, screenSurface: SurfaceTexture) {
    wrapper.eglDisplay =EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    if (wrapper.eglDisplay == EGL14.EGL_NO_DISPLAY) {
        throw RuntimeException("eglGetDisplay failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
    val versions = IntArray(2)
    if (!EGL14.eglInitialize(wrapper.eglDisplay, versions, 0, versions, 1)) {
        throw RuntimeException("eglInitialize failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
    val configsCount = IntArray(1)
    val configs = arrayOfNulls<EGLConfig>(1)
    val configSpec = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_DEPTH_SIZE, 0,
            EGL14.EGL_STENCIL_SIZE, 0,
            EGL14.EGL_NONE
    )
    EGL14.eglChooseConfig(wrapper.eglDisplay, configSpec, 0, configs, 0, 1, configsCount, 0)
    if (configsCount[0] <= 0) {
        throw RuntimeException("eglChooseConfig, failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
    val eglConfig = configs[0]?:throw RuntimeException("eglConfig is null")
    wrapper.eglConfig = eglConfig
    val contextSpec = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
    wrapper.eglContext = EGL14.eglCreateContext(wrapper.eglDisplay, wrapper.eglConfig, sharedContext, contextSpec, 0)
    if (wrapper.eglContext == EGL14.EGL_NO_CONTEXT) {
        throw RuntimeException("eglCreateContext failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
    val value = IntArray(0)
    EGL14.eglQueryContext(wrapper.eglDisplay, wrapper.eglContext, EGL14.EGL_CONTEXT_CLIENT_VERSION, value, 0)
    val surfaceAttr = intArrayOf(EGL14.EGL_NONE)
    wrapper.eglSurface = EGL14.eglCreateWindowSurface(wrapper.eglDisplay, wrapper.eglConfig, screenSurface, surfaceAttr, 0)
    if (wrapper.eglSurface == EGL14.EGL_NO_SURFACE) {
        throw RuntimeException("eglCreateWindowSurface failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
}

internal fun makeCurrent(wrapper: OffScreenGLWrapper) {
    if (!EGL14.eglMakeCurrent(wrapper.eglDisplay,wrapper.eglSurface, wrapper.eglSurface, wrapper.eglContext)) {
        throw RuntimeException("eglMakeCurrent failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
}

internal fun makeCurrent(wrapper: MediaCodecGLWrapper) {
    if (!EGL14.eglMakeCurrent(wrapper.eglDisplay,wrapper.eglSurface, wrapper.eglSurface, wrapper.eglContext)) {
        throw RuntimeException("eglMakeCurrent failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
}

internal fun makeCurrent(wrapper: ScreenGLWrapper) {
    if (!EGL14.eglMakeCurrent(wrapper.eglDisplay,wrapper.eglSurface, wrapper.eglSurface, wrapper.eglContext)) {
        throw RuntimeException("eglMakeCurrent failed:${GLUtils.getEGLErrorString(EGL14.eglGetError())}")
    }
}

internal fun createCameraFrameBuffer(frameBuffer: IntArray, frameBufferTexture: IntArray, width: Int, height: Int) {
    GLES20.glGenFramebuffers(1, frameBuffer, 0)
    GLES20.glGenTextures(1, frameBufferTexture, 0)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, frameBufferTexture[0])
    GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR.toFloat())
    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR.toFloat())
    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE.toFloat())
    GLES20.glTexParameterf(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE.toFloat())
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, frameBuffer[0])
    GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0, GLES20.GL_TEXTURE_2D, frameBufferTexture[0], 0)
    GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, 0)
    GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
    val error = GLES20.glGetError()
    if (error != GLES20.GL_NO_ERROR) {
        throw RuntimeException("createCameraFrameBuffer gl error 0x${Integer.toHexString(error)}")
    }
}

internal fun enableVertex(posLoc: Int, texLoc: Int, shapeBuffer: FloatBuffer, texBuffer: FloatBuffer) {
    GLES20.glEnableVertexAttribArray(posLoc)
    GLES20.glEnableVertexAttribArray(texLoc)
    GLES20.glVertexAttribPointer(posLoc, COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, COORDS_PER_VERTEX * 4, shapeBuffer)
    GLES20.glVertexAttribPointer(texLoc, TEXTURE_COORDS_PER_VERTEX, GLES20.GL_FLOAT, false, TEXTURE_COORDS_PER_VERTEX * 4, texBuffer)
}

internal fun disableVertex(posLoc: Int, texLoc: Int) {
    GLES20.glDisableVertexAttribArray(posLoc)
    GLES20.glDisableVertexAttribArray(texLoc)
}

internal fun createCamera2DProgram(): Int {
    return createProgram(VERTEXSHADER_CAMERA2D, FRAGMENTSHADER_CAMERA2D)
}

internal fun createCameraProgram(): Int {
    return createProgram(VERTEXSHADER, FRAGMENTSHADER_CAMERA)
}

internal fun createMediaCodecProgram(): Int {
    return createProgram(VERTEXSHADER, FRAGMENTSHADER_2D)
}

internal fun createScreenProgram(): Int {
    return createProgram(VERTEXSHADER, FRAGMENTSHADER_2D)
}

internal fun getDrawIndicesBuffer(): ShortBuffer {
    val result =ByteBuffer.allocateDirect(SHORT_SIZE_BYTES * drawIndices.size)
            .order(ByteOrder.nativeOrder())
            .asShortBuffer()
    result.put(drawIndices)
    result.position(0)
    return result
}

internal fun getShapeVerticesBuffer(): FloatBuffer {
    val result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * SquareVertices.size)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    result.put(SquareVertices)
    result.position(0)
    return result
}

internal fun getMediaCodecTextureVerticesBuffer(): FloatBuffer {
    val result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * MediaCodecTextureVertices.size)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    result.put(MediaCodecTextureVertices)
    result.position(0)
    return result
}

internal fun getScreenTextureVerticesBuffer(): FloatBuffer {
    val result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * ScreenTextureVertices.size)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    result.put(ScreenTextureVertices)
    result.position(0)
    return result
}

internal fun getCamera2DTextureVerticesBuffer(directionFlag: Int, cropRatio: Float): FloatBuffer {
    if (directionFlag != -1) {
        val result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * Cam2dTextureVertices.size)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer()
        result.put(Cam2dTextureVertices)
        result.position(0)
        return result
    }
    val buffer = when (directionFlag and 0xF0) {
        ADLiveConstant.FLAG_DIRECTION_ROTATION_90 -> {
            Cam2dTextureVertices_90.clone()
        }
        ADLiveConstant.FLAG_DIRECTION_ROTATION_180 -> {
            Cam2dTextureVertices_180.clone()
        }
        ADLiveConstant.FLAG_DIRECTION_ROTATION_270 -> {
            Cam2dTextureVertices_270.clone()
        }
        else -> {
            Cam2dTextureVertices.clone()
        }
    }
    if ((directionFlag and 0xF0) == ADLiveConstant.FLAG_DIRECTION_ROTATION_0 || (directionFlag and 0xF0) == ADLiveConstant.FLAG_DIRECTION_ROTATION_180) {
        if (cropRatio > 0) {
            buffer[1] = if (buffer[1] == 1.0f) 1.0f - cropRatio else cropRatio
            buffer[3] = if (buffer[3] == 1.0f) 1.0f - cropRatio else cropRatio
            buffer[5] = if (buffer[5] == 1.0f) 1.0f - cropRatio else cropRatio
            buffer[7] = if (buffer[7] == 1.0f) 1.0f - cropRatio else cropRatio
        } else {
            buffer[0] = if (buffer[0] == 1.0f) 1.0f + cropRatio else -cropRatio
            buffer[2] = if (buffer[2] == 1.0f) 1.0f + cropRatio else -cropRatio
            buffer[4] = if (buffer[4] == 1.0f) 1.0f + cropRatio else -cropRatio
            buffer[6] = if (buffer[6] == 1.0f) 1.0f + cropRatio else -cropRatio
        }
    } else {
        if (cropRatio > 0) {
            buffer[0] = if (buffer[0] == 1.0f) 1.0f - cropRatio else cropRatio
            buffer[2] = if (buffer[2] == 1.0f) 1.0f - cropRatio else cropRatio
            buffer[4] = if (buffer[4] == 1.0f) 1.0f - cropRatio else cropRatio
            buffer[6] = if (buffer[6] == 1.0f) 1.0f - cropRatio else cropRatio
        } else {
            buffer[1] = if (buffer[1] == 1.0f) 1.0f + cropRatio else -cropRatio
            buffer[3] = if (buffer[3] == 1.0f) 1.0f + cropRatio else -cropRatio
            buffer[5] = if (buffer[5] == 1.0f) 1.0f + cropRatio else -cropRatio
            buffer[7] = if (buffer[7] == 1.0f) 1.0f + cropRatio else -cropRatio
        }
    }

    fun flip(i : Float) = 1.0f - i

    if ((directionFlag and ADLiveConstant.FLAG_DIRECTION_FLIP_HORIZONTAL) != 0) {
        buffer[0] = flip(buffer[0])
        buffer[2] = flip(buffer[2])
        buffer[4] = flip(buffer[4])
        buffer[6] = flip(buffer[6])
    }
    if ((directionFlag and ADLiveConstant.FLAG_DIRECTION_FLIP_VERTICAL) != 0) {
        buffer[1] = flip(buffer[1])
        buffer[3] = flip(buffer[3])
        buffer[5] = flip(buffer[5])
        buffer[7] = flip(buffer[7])
    }

    val result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * buffer.size)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    result.put(buffer)
    result.position(0)
    return result
}

internal fun getCameraTextureVerticesBuffer(): FloatBuffer {
    val result = ByteBuffer.allocateDirect(FLOAT_SIZE_BYTES * CamTextureVertices.size)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    result.put(CamTextureVertices)
    result.position(0)
    return result
}

internal fun adjustTextureFlip(flipHorizontal: Boolean): FloatBuffer {
    fun flip(i: Float): Float = if (i == 0.0f) 1.0f else 0.0f
    fun getFlip(flipHorizontal: Boolean, flipVertical: Boolean): FloatArray {
        var rotatedTex = Cam2dTextureVertices.clone()
        if (flipHorizontal) {
            rotatedTex = floatArrayOf(
                    flip(rotatedTex[0]), rotatedTex[1],
                    flip(rotatedTex[2]), rotatedTex[3],
                    flip(rotatedTex[4]), rotatedTex[5],
                    flip(rotatedTex[6]), rotatedTex[7]
            )
        }
        if (flipVertical) {
            rotatedTex = floatArrayOf(
                    rotatedTex[0], flip(rotatedTex[1]),
                    rotatedTex[2], flip(rotatedTex[3]),
                    rotatedTex[4], flip(rotatedTex[5]),
                    rotatedTex[6], flip(rotatedTex[7])
            )
        }
        return rotatedTex
    }

    val textureCords = getFlip(flipHorizontal, false)
    val mTextureBuffer = ByteBuffer.allocateDirect(textureCords.size * FLOAT_SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
    mTextureBuffer.clear()
    mTextureBuffer.put(textureCords).position(0)

    return mTextureBuffer
}

internal fun createProgram(vertexShaderCode: String, fragmentShaderCode: String): Int {
    val vertexShader = GLES20.glCreateShader(GLES20.GL_VERTEX_SHADER)
    val fragmentShader = GLES20.glCreateShader(GLES20.GL_FRAGMENT_SHADER)
    GLES20.glShaderSource(vertexShader, vertexShaderCode)
    GLES20.glShaderSource(fragmentShader, fragmentShaderCode)
    val status = IntArray(1)
    GLES20.glCompileShader(vertexShader)
    GLES20.glGetShaderiv(vertexShader, GLES20.GL_COMPILE_STATUS, status, 0)
    if (GLES20.GL_FALSE == status[0]) {
        throw RuntimeException("vertext shader compile,failed:" + GLES20.glGetShaderInfoLog(vertexShader))
    }
    GLES20.glCompileShader(fragmentShader)
    GLES20.glGetShaderiv(fragmentShader, GLES20.GL_COMPILE_STATUS, status, 0)
    if (GLES20.GL_FALSE == status[0]) {
        throw RuntimeException("fragment shader compile,failed:" + GLES20.glGetShaderInfoLog(fragmentShader))
    }
    val program = GLES20.glCreateProgram()
    GLES20.glAttachShader(program, vertexShader)
    GLES20.glAttachShader(program, fragmentShader)
    GLES20.glDeleteShader(vertexShader)
    GLES20.glDeleteShader(fragmentShader)
    GLES20.glLinkProgram(program)
    GLES20.glGetProgramiv(program, GLES20.GL_LINK_STATUS, status, 0)
    if (GLES20.GL_FALSE == status[0]) {
        GLES20.glDeleteProgram(program)
        throw RuntimeException("link program,failed:" + GLES20.glGetProgramInfoLog(program))
    }
    return program
}
