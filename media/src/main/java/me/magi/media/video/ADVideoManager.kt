package me.magi.media.video

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import java.lang.ref.WeakReference

object ADVideoManager {
    private const val STATE_PREVIEW_READY = 1
    private const val STATE_CAMERA_OPEN = 2

    private var mTextureViewHolder: WeakReference<TextureView>? = null
    private var mPreviewSurface: Surface? = null

    private var currentState = 0
    private var targetState = 0

    private var cameraFacing = ADCameraConstant.CAMERA_FACING_BACK
    private var cameraIndex = 0

    fun setTextureView(textureView: TextureView) {
        targetState = STATE_PREVIEW_READY
        mTextureViewHolder = WeakReference(textureView)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                @Suppress("Recycle")
                mPreviewSurface = Surface(surface)
                ADCameraManager.setPreviewSurface(mPreviewSurface)
                currentState = STATE_PREVIEW_READY
                if (targetState == STATE_CAMERA_OPEN) {
                    ADCameraManager.openCamera(cameraFacing, cameraIndex)
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                currentState = 0
                ADCameraManager.setPreviewSurface(null)
                mPreviewSurface?.release()
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }
        }
    }

    fun openCamera(@ADCameraConstant.ADFacingDef cameraFacing: Int, index: Int = 0) {
        if (mTextureViewHolder == null || targetState < STATE_PREVIEW_READY) {
            // 没有设置预览界面
            return
        }
        if (currentState == STATE_CAMERA_OPEN) {
            return
        }
        targetState = STATE_CAMERA_OPEN
        if (mPreviewSurface != null && currentState == STATE_PREVIEW_READY) {
            currentState = STATE_CAMERA_OPEN
            ADCameraManager.openCamera(cameraFacing, index)
        }
    }


}