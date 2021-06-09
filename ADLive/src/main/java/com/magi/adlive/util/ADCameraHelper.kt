package com.magi.adlive.util

import android.content.Context
import android.os.Build
import android.view.MotionEvent
import android.view.Surface
import android.view.WindowManager
import kotlin.math.sqrt

private val verticesData = floatArrayOf(
    // X, Y, Z, U, V
    -1f, -1f, 0f, 0f, 0f,
    1f, -1f, 0f, 1f, 0f,
    -1f, 1f, 0f, 0f, 1f,
    1f, 1f, 0f, 1f, 1f
)

fun getVerticesData(): FloatArray { return verticesData }

fun getCameraOrientation(context: Context): Int {
    val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        context.display
    } else {
        context.getSystemService(WindowManager::class.java)?.defaultDisplay
    }
    return when (display?.rotation) {
        Surface.ROTATION_0 -> 90 //portrait
        Surface.ROTATION_90 -> 0 //landscape
        Surface.ROTATION_180 -> 270 //reverse portrait
        Surface.ROTATION_270 -> 180 //reverse landscape
        else -> 0
    }
}

fun isPortrait(context: Context): Boolean {
    val orientation = getCameraOrientation(context)
    return orientation == 90 || orientation == 270
}

fun getFingerSpacing(event: MotionEvent): Float {
    val x = event.getX(0) - event.getX(1)
    val y = event.getY(0) - event.getY(1)
    return sqrt(x * x + y * y)
}