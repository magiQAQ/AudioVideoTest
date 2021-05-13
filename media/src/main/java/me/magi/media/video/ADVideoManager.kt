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

    private var mTextureViewHolder: WeakReference<TextureView>? = null
    private var mPreviewSurface: Surface? = null


    private var mPushConfig: ADPushConfig? = null

    init {
        ADCameraController.setCameraCallback { errorCode, errorMsg ->
            ADLogUtil.d(errorCode, errorMsg)
        }
        ADLogUtil.d("front: ${ADCameraController.getFrontCameraCount()}, back: ${ADCameraController.getBackCameraCount()}")
    }

    fun setTextureView(textureView: TextureView?) {
        if (textureView == null) {
            ADLogUtil.d("removeTextureView")
            mTextureViewHolder = null
            return
        }
        ADLogUtil.d("setTextureView")
        mTextureViewHolder = WeakReference(textureView)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {

            /**
             * 解决预览拉伸变形问题 start
             */
            private fun scalePreview(viewWidth: Int, viewHeight: Int, previewWidth: Int, previewHeight: Int) {
                val previewRect = RectF(0f, 0f, viewWidth.toFloat(), viewHeight.toFloat())
                var aspect = previewWidth.toFloat() / previewHeight
                if (textureView.context.resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
                    aspect = 1 / aspect
                }
                val targetWidth: Float
                val targetHeight: Float
                if (viewWidth < (viewHeight * aspect)) {
                    targetWidth = viewWidth.toFloat()
                    targetHeight = viewHeight.toFloat() * aspect
                } else {
                    targetWidth = viewWidth.toFloat() / aspect
                    targetHeight = viewHeight.toFloat()
                }
                val targetRect = RectF(0f, 0f, targetWidth, targetHeight)
                val matrix = Matrix()
                matrix.setRectToRect(previewRect, targetRect, Matrix.ScaleToFit.FILL)
                textureView.setTransform(matrix)
            }

            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                ADLogUtil.d("onSurfaceTextureAvailable")
                val previewSurface = Surface(surface)
                mPreviewSurface = previewSurface
                ADCameraDataProcessor.setPreviewSurface(previewSurface)
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
                ADCameraDataProcessor.setPreviewSurface(null)
                mPreviewSurface?.release()
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
            }
        }
    }

    fun setPushConfig(pushConfig: ADPushConfig) {
        mPushConfig = pushConfig
    }

    fun startPreview() {
        if(mPushConfig == null) mPushConfig = ADPushConfig()
        ADLogUtil.d("startPreview")
        ADCameraController.openCamera(
            mPushConfig!!.mCameraFacing,
            mPushConfig!!.mCameraIndex,
            mPushConfig!!.mSize.width,
            mPushConfig!!.mSize.height
        )
    }

    fun stopPreview() {
        ADLogUtil.d("stopPreview")
        ADCameraController.closeCamera()
    }

    fun setFlashState(state: Boolean) {
        ADCameraController.setFlashState(state)
    }

    fun setAutoFocusState(state: Boolean) {
        ADCameraController.setAutoFocus(state)
    }

}