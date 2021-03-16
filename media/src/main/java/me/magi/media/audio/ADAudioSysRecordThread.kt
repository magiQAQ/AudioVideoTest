package me.magi.media.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.*

internal class ADAudioSysRecordThread : Thread() {

    private val TAG = this::class.simpleName

    private val audioInput = MediaRecorder.AudioSource.MIC
    private val sampleRateArray = arrayOf(48000, 44100, 16000, 8000)
    private val channelConfigArray = arrayOf(AudioFormat.CHANNEL_IN_STEREO, AudioFormat.CHANNEL_IN_MONO)

    private var mAudioRecord: AudioRecord? = null
    private lateinit var mRecordBuffer: ByteArray
    private var isRecord = false
    private var isMute = false
    private var mChannelCount: Int = 0
    private var mSampleRate: Int = 0
    private val mAudioEncoding = AudioFormat.ENCODING_PCM_16BIT
    private var mCallback: ADRecordCallback? = null

    internal fun setCallback(callback: ADRecordCallback) {
        mCallback = callback
    }

    internal fun getChannelCount() = mChannelCount
    internal fun getSampleRate() = mSampleRate
    internal fun getBufferSize() = mRecordBuffer.size

    private fun init() {
        Log.i(TAG, "init")
        var recordBufferSize = 0
        var sampleRate = -1
        var channel = -1
        // 为了当前获取音源设备可用的参数
        out@ for (s in sampleRateArray) {
            for (c in channelConfigArray) {
                try {
                    recordBufferSize = AudioRecord.getMinBufferSize(s, c, mAudioEncoding)
                    if (recordBufferSize > 0) {
                        sampleRate = s
                        channel = c
                        break@out
                    } else {
                        Log.e(
                            TAG,
                            "该设备不支持采样率:$s 声道:${if (c == AudioFormat.CHANNEL_IN_STEREO) "双声道" else "单声道"}"
                        )
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        if (recordBufferSize > 0) {
            mRecordBuffer = ByteArray(recordBufferSize)
            Log.i(
                TAG,
                "当前设备采样率:${sampleRate}Hz,${if (channel == AudioFormat.CHANNEL_IN_STEREO) "双声道" else "单声道"}"
            )
            try {
                mAudioRecord =
                    AudioRecord(audioInput, sampleRate, channel, mAudioEncoding, recordBufferSize)
                mSampleRate = sampleRate
                mChannelCount = if (channel == AudioFormat.CHANNEL_IN_STEREO) 2 else 1
                Log.i(TAG, "当前设备采样率:${sampleRate}Hz, 声道数: $mChannelCount")
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "创建AudioRecord失败", e)
            }
        } else {
            Log.i(TAG, "不支持当前设备录音")
            mCallback?.onRecordError(-1, "不支持当前设备录音")
        }

    }

    private fun unInit() {
        Log.i(TAG, "unInit")
        mAudioRecord?.let {
            Log.i(TAG, "开始释放AudioRecord")
            it.stop()
            it.release()
        }
        mAudioRecord = null
    }

    internal fun setMute(isMute: Boolean) {
        this.isMute = isMute
    }

    internal fun stopRecord() {
        isRecord = false
    }

    override fun run() {
        if (isRecord) {
            Log.i(TAG, "录音已经进行")
            return
        }
        init()
        mAudioRecord?.let {
            isRecord = true
            it.startRecording()
            mCallback?.onRecordStart()
        }
        var failCount = 0
        Log.i(TAG, "音频录制开始")
        while (isRecord && mAudioRecord != null && failCount <= 5) {
            val timeMills = System.currentTimeMillis()
            val readSize = mAudioRecord!!.read(mRecordBuffer, 0, mRecordBuffer.size)
            if (readSize <= 0) {
                Log.e(TAG, "读取pcm数据失败, AudioRecord错误码:$readSize")
                failCount++
            } else {
                failCount = 0
                if (isMute) {
                    Arrays.fill(mRecordBuffer, 0.toByte())
                }
                mCallback?.onRecordPcmData(mRecordBuffer, readSize, timeMills)
            }
        }
        if (failCount > 5) {
            Log.i(TAG, "读取pcm数据失败")
            mCallback?.onRecordError(-2, "多次读取pcm数据失败")
        }
        isRecord = false
        Log.i(TAG, "音频录制停止")
        mAudioRecord?.let {
            mCallback?.onRecordStop()
        }
        unInit()
        mCallback?.onRecordFinish()
    }

}