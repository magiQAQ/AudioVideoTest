package me.magi.audioVideoTest.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import java.util.*

class ADAudioSysRecordThread : Thread() {

    private val TAG = this::class.simpleName

    // 音源, 同时支持麦克风和蓝牙耳机
    private val AUDIO_INPUT = MediaRecorder.AudioSource.VOICE_COMMUNICATION

    // 采样率
    private val AUDIO_SAMPLE_RATE_HZ = arrayOf(48000, 44100, 16000, 8000)

    // 音频通道
    private val AUDIO_CHANNEL = arrayOf(AudioFormat.CHANNEL_IN_STEREO, AudioFormat.CHANNEL_IN_MONO)

    // 音频编码
    private val AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT

    private var mAudioRecord: AudioRecord? = null
    private lateinit var mRecordBuffer: ByteArray
    private var isRecord = false
    private var isMute = false
    private var callback = ADRecordSubscriber()

    fun setSubscriber(subscriber: (ADRecordSubscriber.() -> Unit)) {
        callback.subscriber()
    }

    private fun init() {
        var recordBufferSize = 0
        var sampleRate = -1
        var channel = -1
        // 为了当前获取音源设备可用的参数
        for (s in AUDIO_SAMPLE_RATE_HZ) {
            for (c in AUDIO_CHANNEL) {
                try {
                    recordBufferSize = AudioRecord.getMinBufferSize(s, c, AUDIO_ENCODING)
                    if (recordBufferSize > 0) {
                        sampleRate = s
                        channel = c
                        break
                    }
                } catch (e: Exception) {
                    Log.e("AudioRecord", "该设备不支持采样率:$s 声道:${if (c == AudioFormat.CHANNEL_IN_STEREO) "双声道" else "单声道"}")
                }
            }
        }
        if (recordBufferSize > 0) {
            mRecordBuffer = ByteArray(recordBufferSize)
            Log.i(TAG, "当前设备采样率:${sampleRate}Hz,${if (channel == AudioFormat.CHANNEL_IN_STEREO) "双声道" else "单声道"}")
            try {
                mAudioRecord = AudioRecord(AUDIO_INPUT, sampleRate, channel, AUDIO_ENCODING, recordBufferSize)
            } catch (e: IllegalArgumentException) {
                Log.e(TAG, "创建AudioRecord失败", e)
            }
        } else {
            Log.i(TAG, "不支持当前设备录音")
            callback.recordError?.invoke(-1, "不支持当前设备录音")
        }

    }

    private fun unInit() {
        isRecord = false
        mAudioRecord?.let {
            Log.i(TAG, "开始释放AudioRecord")
            it.stop()
            it.release()
        }
        mAudioRecord = null
    }

    fun setMute(isMute: Boolean) {
        this.isMute = isMute
    }

    fun stopRecord() {
        isRecord = false
    }

    override fun run() {
        if (isRecord) {
            Log.i(TAG, "录音已经进行")
            return
        }
        init()
        mAudioRecord?.let {
            it.startRecording()
            isRecord = true
            callback.recordStart?.invoke()
        }

        var failCount = 0
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
                callback.recordPcmData?.invoke(mRecordBuffer, mRecordBuffer.size, timeMills)
            }
        }
        Log.i(TAG, "音频录制停止")
        unInit()
        if (failCount > 5) {
            Log.i(TAG, "读取pcm数据失败")
            callback.recordError?.invoke(-2, "读取pcm数据失败")
        } else {
            callback.recordStop?.invoke()
        }
    }

}