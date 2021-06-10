package com.magi.adlive.librtmp.utils

/**
 * Created by pedro on 8/04/21.
 */
interface ConnectCheckerRtmp {
  fun onConnectionStartedRtmp(rtmpUrl: String)
  fun onConnectionSuccessRtmp()
  fun onConnectionFailedRtmp(reason: String)
  fun onNewBitrateRtmp(bitrate: Long)
  fun onDisconnectRtmp()
  fun onAuthErrorRtmp()
  fun onAuthSuccessRtmp()
}