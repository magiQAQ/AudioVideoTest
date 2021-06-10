package com.magi.adlive.librtmp.rtmp.message

import com.magi.adlive.librtmp.rtmp.chunk.ChunkStreamId
import com.magi.adlive.librtmp.rtmp.chunk.ChunkType
import com.magi.adlive.librtmp.utils.readUInt32
import com.magi.adlive.librtmp.utils.writeUInt32
import java.io.ByteArrayOutputStream
import java.io.InputStream

/**
 * Created by pedro on 21/04/21.
 */
class Acknowledgement(private var sequenceNumber: Int = 0):
    RtmpMessage(BasicHeader(ChunkType.TYPE_0, ChunkStreamId.PROTOCOL_CONTROL)) {

  override fun readBody(input: InputStream) {
    sequenceNumber = input.readUInt32()
  }

  override fun storeBody(): ByteArray {
    val byteArrayOutputStream = ByteArrayOutputStream()
    byteArrayOutputStream.writeUInt32(sequenceNumber)
    return byteArrayOutputStream.toByteArray()
  }

  override fun getType(): MessageType = MessageType.ACKNOWLEDGEMENT

  override fun getSize(): Int = 4

  override fun toString(): String {
    return "Acknowledgement(sequenceNumber=$sequenceNumber)"
  }
}