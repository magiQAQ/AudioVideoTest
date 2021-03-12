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
                val dir = context.getExternalFilesDir(FILE_DIR_NAME)
                val file = File(dir,"fileName")
                fos = FileOutputStream(file)
            }
            onRecordPcmData { data, length, timeMills ->
                try {
                    fos!!.write(data)
                } catch (e: IOException) {
                    Log.e(TAG, "写入失败", e)
                }
            }
            onRecordStop {
                fos!!.flush()
                fos!!.close()
            }
            onRecordError { errorCode, errorMsg ->
                fos?.flush()
                fos?.close()
            }
        }
    }
}