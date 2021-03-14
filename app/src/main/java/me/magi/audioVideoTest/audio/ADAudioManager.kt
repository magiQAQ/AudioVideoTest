package me.magi.audioVideoTest.audio

import android.util.Log
import me.magi.audioVideoTest.utils.getApp
import me.magi.audioVideoTest.utils.getAudioDir
import me.magi.audioVideoTest.utils.pcmFile2WavFile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ADAudioManager {
    private var mRecordThread: ADAudioSysRecordThread? = null
    private val TAG = this::class.simpleName
    private var mCallback: ADAudioCallback? = null

    private var fileNameWithOutSuffix = ""

    fun setRecordCallback(callback: ADAudioCallback) {
        mCallback = callback
    }

    fun startRecordOnlySave(fileNameWithOutSuffix: String) {
        if (mRecordThread != null && mRecordThread!!.isAlive) {
            Log.e(TAG, "用户重复开启未结束的录音")
            mCallback?.onError(3, "当前有未停止的录音")
            return
        }
        mRecordThread = ADAudioSysRecordThread()
        mRecordThread!!.setCallback(OnlySaveRecordCallback(fileNameWithOutSuffix))
        mRecordThread!!.start()
    }

    fun stopRecord() {
        if (mRecordThread == null) return
        if (!mRecordThread!!.isAlive) {
            mRecordThread = null
            return
        }
        mRecordThread!!.stopRecord()
    }

    internal class OnlySaveRecordCallback(private val fileNameWithOutSuffix: String) :
        ADRecordCallback {
        private var fos: FileOutputStream? = null
        private var pcmFile: File? = null
        private var appearIOException = false
        override fun onRecordStart() {
            appearIOException = false
            try {
                val dir = getAudioDir(getApp())
                pcmFile = File(dir, "${fileNameWithOutSuffix}.pcm")
                if (pcmFile!!.exists()) {
                    pcmFile!!.delete()
                }
                pcmFile!!.createNewFile()
                fos = FileOutputStream(pcmFile)
            } catch (e: IOException) {
                appearIOException = true
                mRecordThread!!.stopRecord()
                val msg = "pcm音频文件创建失败"
                Log.e(TAG, msg, e)
                mCallback?.onError(-3, msg)
            }
        }

        override fun onRecordPcmData(data: ByteArray, length: Int, timeMills: Long) {
            try {
                fos?.write(data, 0, length)
            } catch (e: IOException) {
                appearIOException = true
                mRecordThread!!.stopRecord()
                val msg = "pcm数据写入文件失败"
                Log.e(TAG, msg, e)
                mCallback?.onError(-4, msg)
            }
        }

        override fun onRecordStop() {
            try {
                fos?.flush()
                fos?.close()
                fos = null
            } catch (e: IOException) {
                Log.e(TAG, "文件输出流关闭失败", e)
            }
            if (appearIOException || pcmFile == null || !pcmFile!!.exists()) return
            val channelCount = mRecordThread!!.getChannelCount()
            val sampleRate = mRecordThread!!.getSampleRate()
            val bufferSize = mRecordThread!!.getBufferSize()
            val dir = getAudioDir(getApp())
            val wavFile = File(dir, "${fileNameWithOutSuffix}.wav")
            try {
                pcmFile2WavFile(pcmFile!!, wavFile, sampleRate, channelCount, bufferSize)
                pcmFile!!.delete()
            } catch (e: IOException) {
                appearIOException = true
                mCallback?.onError(-5, e.message ?: "")
            }
            if (!appearIOException) mCallback?.onSaveFinish(wavFile)
            mRecordThread = null
        }

        override fun onRecordError(errorCode: Int, errorMsg: String) {
            mCallback?.onError(errorCode, errorMsg)
        }
    }

}