package com.magi.adlive

import android.graphics.Bitmap

interface ScreenShotCallback {
    fun onScreenShot(bitmap: Bitmap?)
}