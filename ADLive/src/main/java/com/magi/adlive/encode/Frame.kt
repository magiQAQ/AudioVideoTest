package com.magi.adlive.encode

import android.graphics.ImageFormat

/**
 * Created by pedro on 17/02/18.
 */
class Frame {
    var buffer: ByteArray
    var offset: Int
    var size: Int
    var orientation = 0
    var isFlip = false
    var format = ImageFormat.NV21 //nv21 or yv12 supported

    /**
     * Used with video frame
     */
    constructor(buffer: ByteArray, orientation: Int, flip: Boolean, format: Int) {
        this.buffer = buffer
        this.orientation = orientation
        isFlip = flip
        this.format = format
        offset = 0
        size = buffer.size
    }

    /**
     * Used with audio frame
     */
    constructor(buffer: ByteArray, offset: Int, size: Int) {
        this.buffer = buffer
        this.offset = offset
        this.size = size
    }
}