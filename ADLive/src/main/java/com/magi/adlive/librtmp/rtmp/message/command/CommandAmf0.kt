package com.magi.adlive.librtmp.rtmp.message.command

import com.magi.adlive.librtmp.rtmp.chunk.ChunkStreamId
import com.magi.adlive.librtmp.rtmp.chunk.ChunkType
import com.magi.adlive.librtmp.rtmp.message.BasicHeader
import com.magi.adlive.librtmp.rtmp.message.MessageType

/**
 * Created by pedro on 21/04/21.
 */
class CommandAmf0(name: String = "", commandId: Int = 0, timestamp: Int = 0, streamId: Int = 0, basicHeader: BasicHeader =
    BasicHeader(ChunkType.TYPE_0, ChunkStreamId.OVER_CONNECTION)
): Command(name, commandId, timestamp, streamId, basicHeader = basicHeader) {
  override fun getType(): MessageType = MessageType.COMMAND_AMF0
}