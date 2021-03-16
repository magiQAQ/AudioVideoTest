package me.magi.media.video

import android.graphics.SurfaceTexture
import android.view.TextureView

class ADVideoManager {
    private var mTextureView: TextureView? = null

    fun setTextureView(textureView: TextureView) {
        mTextureView = textureView
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                TODO("Not yet implemented")
            }

            override fun onSurfaceTextureSizeChanged(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {
                TODO("Not yet implemented")
            }

            override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
                TODO("Not yet implemented")
            }

            override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
                TODO("Not yet implemented")
            }
        }
    }


}