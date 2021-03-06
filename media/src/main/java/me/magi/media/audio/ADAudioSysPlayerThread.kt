package me.magi.media.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.util.Log
import me.magi.media.utils.*
import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException

class ADAudioSysPlayerThread(private val file: File) : Thread() {

    private val TAG = this::class.simpleName

    private var mAudioTrack: AudioTrack? = null
    private lateinit var fis: BufferedInputStream
    private lateinit var mPlayBuffer: ByteArray
    private var isPlaying = false
    private var mChannelConfig: Int = 0
    private var mSampleRate: Int = 0
    private val mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT
    private var mCallback: ADPlayerCallback? = null

    internal fun setCallback(callback: ADPlayerCallback) {
        mCallback = callback
    }

    private fun readFileHeadInfo() {
        try {
            fis = BufferedInputStream(FileInputStream(file))
            val wavHead = getWavFileHeader(fis)
            if (wavHead == null) {
                mCallback?.onPlayError(30, "文件头信息不足44位")
                return
            }
            if (!isWavFile(wavHead)) {
                mCallback?.onPlayError(30, "不是wav文件")
                return
            }
            mChannelConfig = getChannelConfig(wavHead)
            mSampleRate = getSimpleRate(wavHead)
//            val reader = WaveFileReader(file)
//            mChannelConfig = if (reader.numChannels == 1) AudioFormat.CHANNEL_OUT_MONO else AudioFormat.CHANNEL_OUT_STEREO
//            mSampleRate = reader.sampleRate.toInt()
        } catch (e: IOException) {
            mCallback?.onPlayError(30, "文件读取失败")
        }
    }

    private fun init() {
        Log.i(TAG, "init")
        readFileHeadInfo()
        if (mChannelConfig == 0 || mSampleRate <= 0) {
            return
        }
        val bufferSize =
            AudioTrack.getMinBufferSize(mSampleRate, mChannelConfig, mAudioEncoding)
        if (bufferSize <= 0) {
            mCallback?.onPlayError(31, "无法获取有效的缓冲流")
            return
        }
        try {
            mAudioTrack = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .build()
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(mAudioEncoding)
                        .setSampleRate(mSampleRate)
                        .setChannelMask(mChannelConfig)
                        .build()
                )
                .setBufferSizeInBytes(bufferSize)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build()
        } catch (e: Exception) {
            Log.e(TAG, "创建AudioTrack失败")
        }
        mPlayBuffer = ByteArray(bufferSize)
    }

    private fun unInit() {
        Log.i(TAG, "unInit")
        fis.close()
        mAudioTrack?.let {
            Log.i(TAG, "开始释放AudioTrack")
            it.release()
        }
        mAudioTrack = null
    }

    fun stopPlay() {
        isPlaying = false
    }

    override fun run() {
        if (isPlaying) {
            Log.i(TAG, "播放正在进行")
            return
        }
        init()
        mAudioTrack?.let {
            isPlaying = true
            mCallback?.onPlayStart()
        }
        var readSize = 0
        Log.i(TAG, "音频播放开始")
        mAudioTrack?.play()
        while (mAudioTrack!=null && isPlaying && fis.read(mPlayBuffer).also { readSize = it } > 0) {
            mAudioTrack!!.write(mPlayBuffer, 0, readSize)
            mCallback?.onPlayTime(0,0)
        }
        isPlaying = false
        Log.i(TAG, "音频播放结束")
        mAudioTrack?.let {
            mCallback?.onPlayEnd()
        }
        unInit()
        mCallback?.onPlayFinish()
    }
}