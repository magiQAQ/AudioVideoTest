package com.magi.adlive.encode.video

import android.media.MediaCodecInfo.CodecCapabilities.*

enum class FormatVideoEncoder {

    YUV420FLEXIBLE, YUV420PLANAR, YUV420SEMIPLANAR, YUV420PACKEDPLANAR, YUV420PACKEDSEMIPLANAR,
    YUV422FLEXIBLE, YUV422PLANAR, YUV422SEMIPLANAR, YUV422PACKEDPLANAR, YUV422PACKEDSEMIPLANAR,
    YUV444FLEXIBLE, YUV444INTERLEAVED, SURFACE,
    //take first valid color for encoder (YUV420PLANAR, YUV420SEMIPLANAR or YUV420PACKEDPLANAR)
    YUV420Dynamical;

    fun getFormatCodec(): Int {
        return when (this) {
            YUV420FLEXIBLE -> COLOR_FormatYUV420Flexible
            YUV420PLANAR -> COLOR_FormatYUV420Planar
            YUV420SEMIPLANAR -> COLOR_FormatYUV420SemiPlanar
            YUV420PACKEDPLANAR -> COLOR_FormatYUV420PackedPlanar
            YUV420PACKEDSEMIPLANAR -> COLOR_FormatYUV420PackedSemiPlanar
            YUV422FLEXIBLE -> COLOR_FormatYUV422Flexible
            YUV422PLANAR -> COLOR_FormatYUV422Planar
            YUV422SEMIPLANAR -> COLOR_FormatYUV422SemiPlanar
            YUV422PACKEDPLANAR -> COLOR_FormatYUV422PackedPlanar
            YUV422PACKEDSEMIPLANAR -> COLOR_FormatYUV422PackedSemiPlanar
            YUV444FLEXIBLE -> COLOR_FormatYUV444Flexible
            YUV444INTERLEAVED -> COLOR_FormatYUV444Interleaved
            SURFACE -> COLOR_FormatSurface
            YUV420Dynamical -> -1
        }
    }
}