package com.magi.adlive.encode

import android.media.MediaCodecInfo
import android.media.MediaCodecList
import android.os.Build
import java.util.*

private const val TAG = "CodecUtil"

const val H264_MIME = "video/avc"
const val H265_MIME = "video/hevc"
const val AAC_MIME = "audio/mp4a-latm"
const val VORBIS_MIME = "audio/ogg"
const val OPUS_MIME = "audio/opus"

fun getAllCodecs(): MutableList<MediaCodecInfo> {
    val mediaCodecInfoList = arrayListOf<MediaCodecInfo>()
    val mediaCodecInfos = MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos
    mediaCodecInfoList.addAll(mediaCodecInfos)
    return mediaCodecInfoList
}

fun getAllHardwareEncoders(mime: String): MutableList<MediaCodecInfo> {
    val mediaCodecInfoList = getAllEncoder(mime)
    val hardwareCodes = arrayListOf<MediaCodecInfo>()
    for (mediaCodecInfo in mediaCodecInfoList) {
        if (isHardwareAccelerated(mediaCodecInfo)) {
            hardwareCodes.add(mediaCodecInfo)
        }
    }
    return mediaCodecInfoList
}

fun getAllEncoder(mime: String): MutableList<MediaCodecInfo> {
    val mediaInfoList = arrayListOf<MediaCodecInfo>()
    val allCodecs = getAllCodecs()
    for (info in allCodecs) {
        if (!info.isEncoder) continue
        val types = info.supportedTypes
        for (type in types) {
            if (type.lowercase() == mime) mediaInfoList.add(info)
        }
    }
    return mediaInfoList
}

/**
 * 检查传入的codec是否支持硬件编码或解码
 */
private fun isHardwareAccelerated(codecInfo: MediaCodecInfo): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return codecInfo.isHardwareAccelerated
    }
    return !isSoftwareOnly(codecInfo)
}

/**
 * 检查传入的codec是否只支持软件编码或解码
 */
private fun isSoftwareOnly(codecInfo: MediaCodecInfo): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        return codecInfo.isSoftwareOnly
    }
    val name = codecInfo.name.lowercase(Locale.getDefault())
    if (name.startsWith("arc.")) return false
    return (name.startsWith("omx.google.")
            || name.startsWith("omx.ffmpeg.")
            || name.startsWith("omx.sec.") && name.contains(".sw.")
            || name == "omx.qcom.video.decoder.hevcswvdec" || name.startsWith("c2.android.")
            || name.startsWith("c2.google.")
            || !name.startsWith("omx.") && !name.startsWith("c2."))
}