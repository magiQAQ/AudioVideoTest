package com.magi.adlive.encode.audio

import com.magi.adlive.encode.Frame

interface GetMicrophoneData {
    fun inputPCMData(frame: Frame)
}