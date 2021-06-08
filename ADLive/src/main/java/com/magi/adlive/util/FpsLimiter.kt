package com.magi.adlive.util

class FpsLimiter {

    private var lastFrameTime = System.currentTimeMillis()
    private var duration = 1000 / 30

    fun setFps(fps: Int) {
        lastFrameTime = System.currentTimeMillis()
        duration = 1000 / fps
    }

    fun limitFPS(): Boolean {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastFrameTime > duration) {
            lastFrameTime = currentTime
            return false
        }
        return true
    }

}