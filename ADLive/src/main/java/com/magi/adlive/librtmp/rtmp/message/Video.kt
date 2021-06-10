package com.magi.adlive.librtmp.rtmp.message

import com.magi.adlive.librtmp.flv.FlvPacket
import com.magi.adlive.librtmp.rtmp.chunk.ChunkStreamId
import com.magi.adlive.librtmp.rtmp.chunk.ChunkType
import java.io.InputStream

/**
 * Created by pedro on 21/04/21.
 */
class Video(private val flvPacket: FlvPacket = FlvPacket(), streamId: Int = 0): RtmpMessage(BasicHeader(ChunkType.TYPE_0, ChunkStreamId.VIDEO)) {

  init {
    header.messageStreamId = streamId
    header.timeStamp = flvPacket.timeStamp.toInt()
    header.messageLength = flvPacket.length
  }

  override fun readBody(input: InputStream) {
  }

  override fun storeBody(): ByteArray = flvPacket.buffer

  override fun getType(): MessageType = MessageType.VIDEO

  override fun getSize(): Int = flvPacket.length

  override fun toString(): String {
    return "Video, size: ${getSize()}"
  }
}