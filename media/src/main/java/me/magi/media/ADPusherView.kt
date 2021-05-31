package me.magi.media

import android.content.Context
import android.graphics.SurfaceTexture
import android.util.AttributeSet
import android.util.Size
import android.view.TextureView
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.widget.FrameLayout
import me.magi.media.utils.ADLiveConstant
import me.magi.media.widget.AspectTextureView

class ADPusherView: FrameLayout, TextureView.SurfaceTextureListener{

    constructor(context: Context): super(context)

    constructor(context: Context, attrs: AttributeSet): super(context, attrs)

    private var mTextureView: AspectTextureView

    init {
        val textureView = AspectTextureView(context)
        textureView.layoutParams = LayoutParams(MATCH_PARENT, MATCH_PARENT)
        textureView.keepScreenOn = true
        addView(textureView)
        textureView.surfaceTextureListener = this
        mTextureView = textureView
    }

    fun setPreviewRatio(@ADLiveConstant.ADPreviewModeDef mode: Int, ratioSize: Size) {
        mTextureView.setAspectRatio(mode, ratioSize)
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {
        TODO("Not yet implemented")
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        TODO("Not yet implemented")
    }

    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {
        TODO("Not yet implemented")
    }

}