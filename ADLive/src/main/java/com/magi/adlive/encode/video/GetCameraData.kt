package com.magi.adlive.encode.video

import com.magi.adlive.encode.Frame

interface GetCameraData {
    fun inputYUVData(frame: Frame)
}