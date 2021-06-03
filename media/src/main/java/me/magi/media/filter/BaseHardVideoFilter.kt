package me.magi.media.filter

import me.magi.media.utils.getDrawIndicesBuffer
import java.nio.FloatBuffer
import java.nio.ShortBuffer

internal abstract class BaseHardVideoFilter {
    protected var SIZE_WIDTH = 0
    protected var SIZE_HEIGHT = 0
    protected var directionFlag = -1
    protected lateinit var drawIndicesBuffer: ShortBuffer

    fun onInit(VWidth: Int, VHeight: Int) {
        SIZE_WIDTH = VWidth
        SIZE_HEIGHT = VHeight
        drawIndicesBuffer = getDrawIndicesBuffer()
    }

    fun onDraw(cameraTexture: Int, targetFrameBuffer: Int, shapeBuffer: FloatBuffer, textureBuffer: FloatBuffer){}

    fun onDestroy(){}

    fun onDirectionUpdate(directionFlag: Int){this.directionFlag = directionFlag}
}