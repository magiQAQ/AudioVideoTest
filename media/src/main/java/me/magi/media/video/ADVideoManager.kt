package me.magi.media.video

import android.content.Context
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraManager
import android.view.TextureView
import me.magi.media.utils.getApp
import java.lang.ref.WeakReference

object ADVideoManager {
    private var mTextureViewHolder: WeakReference<TextureView>? = null
    private val cameraManager by lazy { getApp().getSystemService(Context.CAMERA_SERVICE) as CameraManager }

    fun setTextureView(textureView: TextureView) {
        mTextureViewHolder = WeakReference(textureView)
        textureView.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(
                surface: SurfaceTexture,
                width: Int,
                height: Int
            ) {

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