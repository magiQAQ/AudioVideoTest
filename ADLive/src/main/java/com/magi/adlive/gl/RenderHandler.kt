package com.magi.adlive.gl

class RenderHandler {

    private var fboId = intArrayOf(0)
    private var rboId = intArrayOf(0)
    private var texId = intArrayOf(0)

    fun getTexId() : IntArray { return texId }

    fun getFboId(): IntArray { return fboId }

    fun getRboId(): IntArray { return rboId }

    fun setTexId(texId: IntArray) { this.texId = texId }

    fun setFboId(fboId: IntArray) { this.fboId = fboId }

    fun setRboId(rboId: IntArray) { this.rboId = rboId }

}