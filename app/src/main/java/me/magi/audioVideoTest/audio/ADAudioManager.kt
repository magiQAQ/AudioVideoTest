package me.magi.audioVideoTest.audio

import android.content.Context
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ADAudioManager {
    var mRecordThread: ADAudioSysRecordThread? = null
    private const val FILE_DIR_NAME = "AUDIO_TEST"
    private var fos: FileOutputStream?=null
    private val TAG = this::class.simpleName

    fun startRecordAndSaveLocal(context: Context, fileName: String) {
        mRecordThread = ADAudioSysRecordThread()
        mRecordThread?.setSubscriber {
            onRecordStart {
                try {
                    val dir = context.getExternalFilesDir(FILE_DIR_NAME)
                    val file = File(dir, fileName)
                    fos = FileOutputStream(file)
                } catch (e: IOException) {
                    Log.e(TAG, "pcm音频文件创建失败", e)
                }
            }
            onRecordPcmData { data, length, timeMills ->
                try {
                    fos!!.write(data)
                } catch (e: IOException) {
                    Log.e(TAG, "pcm数据写入文件失败", e)
                }
            }
            onRecordStop {
                try {
                    fos!!.flush()
                    fos!!.close()
                } catch (e: IOException) {
                    Log.e(TAG, "文件输出流关闭失败", e)
                }
            }
            onRecordError { errorCode, errorMsg ->
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