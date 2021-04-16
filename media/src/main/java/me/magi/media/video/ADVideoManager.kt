package me.magi.media.video

import android.annotation.SuppressLint
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import java.lang.ref.WeakReference

object ADVideoManager {
    private var mTextureViewHolder: WeakReference<TextureView>? = null


    fun setTextureView(textureView: TextureView) {
        mTextureViewHolder = WeakReference(textureView)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            private var mPreviewSurface: Surface? = null

            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                @Suppress("Recycle")
                mPreviewSurface = Surface(surface)
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                mPreviewSurface?.release()
                return false
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {

            }
        }
    }


}