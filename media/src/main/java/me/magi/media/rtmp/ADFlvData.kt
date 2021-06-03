package me.magi.media.rtmp

internal class ADFlvData {
    internal companion object{
        const val FLV_RTMP_PACKET_TYPE_VIDEO = 9
        const val FLV_RTMP_PACKET_TYPE_AUDIO = 8
        const val FLV_RTMP_PACKET_TYPE_INFO = 18
        const val NALU_TYPE_IDR = 5
    }

    var droppable = false

    var dts = 0 //解码时间戳

    var byteBuffer: ByteArray? = null //数据

    var size = 0 //字节长度

    var flvTagType = 0 //视频和音频的分类

    var videoFrameType = 0

    fun isKeyframe(): Boolean = videoFrameType == NALU_TYPE_IDR

}