package com.magi.adlive.util

import android.graphics.Point
import android.graphics.PointF
import android.opengl.GLES20
import android.opengl.Matrix
import com.magi.adlive.model.AspectRatioMode

fun calculateViewPort(
    keepAspectRatio: Boolean, mode: Int, previewWidth: Int,
    previewHeight: Int, streamWidth: Int, streamHeight: Int
) {
    val pair = getViewport(
        keepAspectRatio, mode, previewWidth, previewHeight, streamWidth, streamHeight
    )
    GLES20.glViewport(pair.first.x, pair.first.y, pair.second.x, pair.second.y)
}

fun getViewport(
    keepAspectRatio: Boolean,
    mode: Int,
    previewWidth: Int,
    previewHeight: Int,
    streamWidth: Int, streamHeight: Int
): Pair<Point, Point> {
    return if (keepAspectRatio) {
        val streamAspectRatio = streamWidth.toFloat() / streamHeight.toFloat()
        val previewAspectRatio = previewWidth.toFloat() / previewHeight.toFloat()
        var xo = 0
        var yo = 0
        var xf = previewWidth
        var yf = previewHeight
        if ((streamAspectRatio > 1f && previewAspectRatio > 1f && streamAspectRatio > previewAspectRatio)
            || (streamAspectRatio < 1f && previewAspectRatio < 1f && streamAspectRatio > previewAspectRatio)
            || (streamAspectRatio > 1f && previewAspectRatio < 1f)
        ) {
            if (mode == AspectRatioMode.Adjust.id || mode == AspectRatioMode.AdjustRotate.id) {
                yf = streamHeight * previewWidth / streamWidth
                yo = (yf - previewHeight) / -2
            } else {
                xf = streamWidth * previewHeight / streamHeight
                xo = (xf - previewWidth) / -2
            }
        } else if ((streamAspectRatio > 1f && previewAspectRatio > 1f && streamAspectRatio < previewAspectRatio)
            || (streamAspectRatio < 1f && previewAspectRatio < 1f && streamAspectRatio < previewAspectRatio)
            || (streamAspectRatio < 1f && previewAspectRatio > 1f)
        ) {
            if (mode == AspectRatioMode.Adjust.id || mode == AspectRatioMode.AdjustRotate.id) {
                xf = streamWidth * previewHeight / streamHeight
                xo = (xf - previewWidth) / -2
            } else {
                yf = streamHeight * previewWidth / streamWidth
                yo = (yf - previewHeight) / -2
            }
        }
        Pair(Point(xo, yo), Point(xf, yf))
    } else {
        Pair(Point(0, 0), Point(previewWidth, previewHeight))
    }
}

fun processMatrix(
    rotation: Int, width: Int, height: Int, isPreview: Boolean,
    isPortrait: Boolean, flipStreamHorizontal: Boolean, flipStreamVertical: Boolean,
    mode: Int, MVPMatrix: FloatArray
) {
    var uRotation = rotation
    var scale: PointF
    if (mode == AspectRatioMode.Adjust.id || mode == AspectRatioMode.FillRotate.id) {
        // stream rotation is enabled
        scale = getScale(uRotation, width, height, isPortrait, isPreview)
        if (!isPreview && !isPortrait) uRotation += 90
    } else {
        scale = PointF(1f, 1f)
    }
    if (!isPreview) {
        val xFlip = if (flipStreamHorizontal) -1f else 1f
        val yFlip = if (flipStreamVertical) -1f else 1f
        scale = PointF(scale.x * xFlip, scale.y * yFlip)
    }
    updateMatrix(uRotation, scale, MVPMatrix)
}

private fun updateMatrix(rotation: Int, scale: PointF, MVPMatrix: FloatArray) {
    Matrix.setIdentityM(MVPMatrix, 0)
    Matrix.scaleM(MVPMatrix, 0, scale.x, scale.y, 1f)
    Matrix.rotateM(MVPMatrix, 0, rotation.toFloat(), 0f, 0f, -1f)
}

private fun getScale(
    rotation: Int,
    width: Int,
    height: Int,
    isPortrait: Boolean,
    isPreview: Boolean
): PointF {
    var scaleX = 1f
    var scaleY = 1f
    if (!isPreview) {
        if (isPortrait && rotation != 0 && rotation != 180) { //portrait
            val adjustedWidth = width * (width / height.toFloat())
            scaleY = adjustedWidth / height
        } else if (!isPortrait && rotation != 90 && rotation != 270) { //landscape
            val adjustedWidth = height * (height / width.toFloat())
            scaleX = adjustedWidth / width
        }
    }
    return PointF(scaleX, scaleY)
}
