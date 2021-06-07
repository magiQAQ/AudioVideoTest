package com.magi.adlive.widget

import android.graphics.SurfaceTexture
import android.view.TextureView

abstract class ADLiveGLView: TextureView, ADLiveGLInterface, Runnable, SurfaceTexture.OnFrameAvailableListener, TextureView.SurfaceTextureListener{



}