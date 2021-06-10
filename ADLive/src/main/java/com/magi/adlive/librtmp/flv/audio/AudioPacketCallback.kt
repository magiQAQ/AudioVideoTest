package com.magi.adlive.librtmp.flv.audio

import com.magi.adlive.librtmp.flv.FlvPacket

/**
 * Created by pedro on 29/04/21.
 */
interface AudioPacketCallback {
  fun onAudioFrameCreated(flvPacket: FlvPacket)
}