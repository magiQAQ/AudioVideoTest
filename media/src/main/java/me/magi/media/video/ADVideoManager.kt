package me.magi.media.video

import android.content.res.Configuration
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import me.magi.media.utils.ADLogUtil
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

    private var mPushConfig: ADPushConfig? = null

    init {
        ADCameraController.setCameraCallback { errorCode, errorMsg ->
            ADLogUtil.d(errorCode, errorMsg)
        }
        ADLogUtil.d("front: ${ADCameraController.getFrontCameraCount()}, back: ${ADCameraController.getBackCameraCount()}")
    }

    fun setPushConfig(pushConfig: ADPushConfig) {
        mPushConfig = pushConfig
    }

    fun setTextureView(textureView: TextureView) {
        ADLogUtil.d("setTextureView")
        targetState = STATE_PREVIEW_READY
        mTextureViewHolder = WeakReference(textureView)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {

            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                ADLogUtil.d("onSurfaceTextureAvailable")
                // 解决预览拉伸变形问题 start
                val previewRect = RectF(0f, 0f, width.toFloat(), height.toFloat())
                val previewWidth = mPushConfig?.getWidth()?:1280
                val previewHeight = mPushConfig?.getHeight()?:720
                var aspect = previewWidth.toFloat()/previewHeight
                if (textureView.context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    aspect = 1/ aspect
                }
                val targetWidth: Float
                val targetHeight: Float
                if (width < (height * aspect)) {
                    targetWidth = width.toFloat()
                    targetHeight = height.toFloat() * aspect
                } else {
                    targetWidth = width.toFloat() / aspect
                    targetHeight = height.toFloat()
                }
                val targetRect = RectF(0f, 0f, targetWidth, targetHeight)
                val matrix = Matrix()
                matrix.setRectToRect(previewRect, targetRect, Matrix.ScaleToFit.FILL)
                textureView.setTransform(matrix)
                // 解决预览拉伸变形问题 end
                surface.setDefaultBufferSize(targetWidth.toInt(), targetHeight.toInt())
                mPreviewSurface = Surface(surface)
                ADCameraController.setPreviewSurface(mPreviewSurface)
                currentState = STATE_PREVIEW_READY
                if (targetState == STATE_CAMERA_OPEN) {
                    ADCameraController.openCamera(
                        cameraFacing,
                        cameraIndex,
                        previewWidth,
                        previewHeight
                    )
                }
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                ADLogUtil.d("onSurfaceTextureSizeChanged")
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                ADLogUtil.d("onSurfaceTextureDestroyed")
                currentState = 0
                ADCameraController.setPreviewSurface(null)
                mPreviewSurface?.release()
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }
        }
    }

    fun startPreview(@ADCameraConstant.ADFacingDef cameraFacing: Int, index: Int = 0) {
        if (mTextureViewHolder == null || targetState < STATE_PREVIEW_READY) {
            // 没有设置预览界面
            return
        }
        if (currentState == STATE_CAMERA_OPEN) {
            return
        }
        val previewWidth = 1280
        val preHeight = 720
        ADLogUtil.d("startPreview")
        targetState = STATE_CAMERA_OPEN
        if (mPreviewSurface != null && currentState == STATE_PREVIEW_READY) {
            currentState = STATE_CAMERA_OPEN
            ADCameraController.openCamera(cameraFacing, index, previewWidth, preHeight)
        }
    }

    fun stopPreview() {
        targetState = if (mTextureViewHolder != null) {
            STATE_PREVIEW_READY
        } else {
            0
        }
        if (currentState == STATE_CAMERA_OPEN) {
            ADLogUtil.d("stopPreview")
            ADCameraController.closeCamera()
            currentState = STATE_PREVIEW_READY
        }
    }

    fun setFlashState(state: Boolean) {
        ADCameraController.setFlashState(state)
    }

    fun setAutoFocusState(state: Boolean) {
        ADCameraController.setAutoFocus(state)
    }

}