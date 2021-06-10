package com.magi.adlive.encode.video

import com.magi.adlive.model.Facing

interface ADCameraCallback {
    fun onCameraChanged(facing: Facing)
    fun onCameraError(error: String)
}