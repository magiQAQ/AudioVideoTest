package com.magi.adlive.widget

import android.graphics.SurfaceTexture
import android.view.Surface
import com.magi.adlive.ScreenShotCallback
import com.magi.adlive.gl.filter.BaseFilterRender

interface ADLiveGLInterface {

    /**
     * 用于初始化一些必要的类
     */
    fun init()

    /**
     * 设置用于OpenGL的编码器
     */
    fun setEncoderSize(width: Int, height: Int)

    /**
     * 获取OpenGL生成的SurfaceTexture,应该在开始渲染后被调用
     */
    fun getSurfaceTexture(): SurfaceTexture

    /**
     * 获取OpenGL生成的Surface,应该在开始渲染后被调用
     */
    fun getSurface(): Surface

    /**
     * 将MediaCodec生成的Surface设置给OpenGL
     * 这个Surface会复制OpenGL上Surface的像素,将其用于编码
     */
    fun addMediaCodecSurface(surface: Surface)

    /**
     * 移除MediaCodec生成的Surface
     */
    fun removeMediaCodecSurface()

    /**
     * 从OpenGL中截图,通过回调返回Bitmap
     */
    fun takePhoto(screenShotCallback: ScreenShotCallback)

    /**
     * 给流设置滤镜
     * 这个滤镜需要继承于BaseFilterRender
     */
    fun setFilter(filterPosition: Int, baseFilterRender: BaseFilterRender)

    /**
     * 从最开始的位置插入滤镜
     */
    fun setFilter(baseFilterRender: BaseFilterRender)

    /**
     * 开启或关闭抗锯齿
     */
    fun enableAA(enable: Boolean)

    fun setRotation(rotation: Int)

    /**
     * 流是否水平反转
     */
    fun setIsStreamHorizontalFlip(flip: Boolean)

    /**
     * 流是否垂直反转
     */
    fun setIsStreamVerticalFlip(flip: Boolean)

    /**
     * 抗锯齿是否开启
     */
    fun isAAEnabled(): Boolean

    /**
     * 启动 Opengl 渲染。
     */
    fun start()

    /**
     * 停止 Opengl 渲染。
     */
    fun stop()

    fun setFps(fps: Int)

    /**
     * 该产品始终发送黑色图像。
     */
    fun muteVideo()

    fun unMuteVideo()

    fun isVideoMuted()

    /**
     * @param force 渲染最后一帧。 这对于显示模式很有用，可以继续生成视频帧。 在其他模式下不推荐
     */
    fun setForceRender(force: Boolean)
}