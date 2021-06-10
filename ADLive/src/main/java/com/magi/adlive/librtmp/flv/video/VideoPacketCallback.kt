package com.magi.adlive.librtmp.flv.video

import com.magi.adlive.librtmp.flv.FlvPacket

/**
 * Created by pedro on 29/04/21.
 */
interface VideoPacketCallback {
  fun onVideoFrameCreated(flvPacket: FlvPacket)
}