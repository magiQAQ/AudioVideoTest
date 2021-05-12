package me.magi.media.video

import android.media.ImageReader
import android.os.Handler
import android.view.Surface

object ADCameraDataProcessor {

    private var mPreviewSurface: Surface? = null

    fun getImageReaderSurface(width: Int, height: Int, handler: Handler): Surface {
        val imageReader = ADImageReaderHolder.getInstance(width, height)
        imageReader.setOnImageAvailableListener(imageAvailableListener, handler)
        return imageReader.surface
    }

    fun release() {
        ADImageReaderHolder.releaseCurrentImageReader()
    }

    private val imageAvailableListener = { imageReader: ImageReader ->
        val image = imageReader.acquireNextImage()
        if (image != null) {
            val planes = image.planes
            val yPlane = planes[0]
            val uPlane = planes[1]
            val vPlane = planes[2]
            val yBuffer = yPlane.buffer
            val uBuffer = uPlane.buffer
            val vBuffer = vPlane.buffer
            // 处理yuv420数据 start

            // 处理yuv420数据 end
            image.close()
        }
    }

    fun setPreviewSurface(previewSurface: Surface) {
        mPreviewSurface = previewSurface
    }
}