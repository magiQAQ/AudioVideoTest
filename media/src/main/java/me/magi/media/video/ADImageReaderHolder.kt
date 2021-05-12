package me.magi.media.video

import android.graphics.ImageFormat
import android.media.ImageReader

internal object ADImageReaderHolder {
    private var mImageReader: ImageReader? = null

    fun getInstance(width: Int, height: Int): ImageReader {
        var imageReader = mImageReader
        if (imageReader == null) {
            imageReader = newInstance(width, height)
            mImageReader = imageReader
        } else if (!imageReader.surface.isValid || imageReader.width != width || imageReader.height != height) {
            imageReader.close()
            imageReader = newInstance(width, height)
            mImageReader = imageReader
        }
        return imageReader
    }

    fun releaseCurrentImageReader() {
        mImageReader?.close()
    }

    private fun newInstance(width: Int, height: Int): ImageReader {
        return ImageReader.newInstance(width, height, ImageFormat.YUV_420_888, 2)
    }
}