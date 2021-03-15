package me.magi.media.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class ADAudioSysPlayerThread(private val file: File) : Thread() {

    private val TAG = this::class.simpleName

    private var mCallback: ADPlayerCallback? = null

    private var mAudioTrack: AudioTrack? = null

    private var channelType: Int = 0
    private var sampleRate: Int = 0
    private var bufferSize: Int = 0

    private fun readFileHeadInfo() {
        try {
            val fis = FileInputStream(file)
            val wavHead = ByteArray(44)
            val readSize = fis.read(wavHead, 0, wavHead.size)
            fis.close()
            if (readSize != 44) {
                mCallback?.onPlayError(30, "文件头信息不足44位")
                return
            }
            if (!isWavFile(wavHead)) {
                mCallback?.onPlayError(30, "不是标准的wav文件")
            }
            channelType = getChannelType(wavHead)
            sampleRate = getSimpleRate(wavHead)
            bufferSize =
                AudioTrack.getMinBufferSize(sampleRate, channelType, AudioFormat.ENCODING_PCM_16BIT)
            if (bufferSize <= 0) {
                mCallback?.onPlayError(31, "无法获取有效的缓冲流")
                return
            }
        } catch (e: IOException) {
            mCallback?.onPlayError(30, "文件读取失败")
        }
    }

    private fun isWavFile(wavHead: ByteArray): Boolean {
        var riff = ""
        var wave = ""
        for (index in 0..3) {
            riff += wavHead[index].toChar()
        }
        for (index in 8..11) {
            wave += wavHead[index].toChar()
        }
        return riff == "RIFF" && wave == "WAVE"
    }

    private fun getChannelType(wavHead: ByteArray): Int {
        return if (wavHead[22].toInt() == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
    }

    private fun getSimpleRate(wavHead: ByteArray): Int {
        return wavHead[24].toInt() + wavHead[25].toInt() shl 8 + wavHead[26].toInt() shl 16 + wavHead[27].toInt() shl 24
    }

    private fun init() {
        Log.i(TAG, "init")
        mAudioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(channelType)
                    .build()
            )
            .setBufferSizeInBytes(bufferSize)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
    }

    private fun unInit() {
        Log.i(TAG, "unInit")
        mAudioTrack?.let {
            Log.i(TAG, "开始释放AudioTrack")
            it.stop()
            it.release()
        }
        mAudioTrack = null
    }
}