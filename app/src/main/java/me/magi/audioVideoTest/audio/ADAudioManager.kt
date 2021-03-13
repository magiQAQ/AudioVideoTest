package me.magi.audioVideoTest.audio

import android.content.Context
import android.util.Log
import me.magi.audioVideoTest.utils.getAudioDir
import me.magi.audioVideoTest.utils.pcmFile2WavFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ADAudioManager {
    var mRecordThread: ADAudioSysRecordThread? = null
    private val TAG = this::class.simpleName
    private var mCallback: ADAudioRecordCallback?= null

    fun setRecordCallback(callback: ADAudioRecordCallback) {
        mCallback = callback
    }

    fun startRecordAndSaveLocal(context: Context, fileNameWithOutSuffix: String) {
        if (mRecordThread!=null && mRecordThread!!.isAlive) {
            Log.e(TAG, "用户重复开启未结束的录音")
            mCallback?.onError(3, "当前有未停止的录音")
            return
        }
        var fos: FileOutputStream? = null
        var pcmFile: File? = null
        mRecordThread = ADAudioSysRecordThread()
        mRecordThread?.setSubscriber {
            onRecordStart {
                try {
                    val dir = getAudioDir(context)
                    pcmFile = File(dir, "${fileNameWithOutSuffix}.pcm")
                    if (pcmFile!!.exists()) {
                        pcmFile!!.delete()
                    }
                    pcmFile!!.createNewFile()
                    fos = FileOutputStream(pcmFile)
                } catch (e: IOException) {
                    mRecordThread!!.stopRecord()
                    val msg = "pcm音频文件创建失败"
                    Log.e(TAG, msg, e)
                    mCallback?.onError(-3, msg)
                }
            }
            onRecordPcmData { data, length, timeMills ->
                try {
                    fos?.write(data, 0, length)
                } catch (e: IOException) {
                    mRecordThread!!.stopRecord()
                    val msg = "pcm数据写入文件失败"
                    Log.e(TAG, msg, e)
                    mCallback?.onError(-4, msg)
                }
            }
            onRecordStop {
                try {
                    fos?.flush()
                    fos?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "文件输出流关闭失败", e)
                }
                if (pcmFile == null || !pcmFile!!.exists()) {
                    return@onRecordStop
                }
                val channelCount = mRecordThread!!.getChannelCount()
                val sampleRate = mRecordThread!!.getSampleRate()
                val bufferSize = mRecordThread!!.getBufferSize()
                val dir = getAudioDir(context)
                val wavFile = File(dir, "${fileNameWithOutSuffix}.wav")
                try {
                    pcmFile2WavFile(pcmFile!!, wavFile, sampleRate, channelCount, bufferSize)
                    mCallback?.onRecordFinish(wavFile)
                } catch (e: IOException) {
                    mCallback?.onError(-5, e.message?:"")
                }
            }
            onRecordError { errorCode, errorMsg ->
                mCallback?.onError(errorCode, errorMsg)
                try {
                    fos?.flush()
                    fos?.close()
                } catch (e: IOException) {
                    Log.e(TAG, "文件输出流关闭失败", e)
                }
            }
        }
    }


}