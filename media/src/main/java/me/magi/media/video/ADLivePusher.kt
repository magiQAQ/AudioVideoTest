package me.magi.media.video

import me.magi.media.widget.ADRenderView

class ADLivePusher {

    private var cameraController = ADCameraController()
    private var mRenderView: ADRenderView? = null

    fun setRenderView(renderView: ADRenderView) {
        mRenderView = renderView
    }



}