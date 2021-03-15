package me.magi.audioVideoTest

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import me.magi.media.audio.ADAudioCallback
import me.magi.media.audio.ADAudioManager
import me.magi.media.utils.showToast
import java.io.File
import java.lang.Exception

class MainActivity : AppCompatActivity() {
    private var wavFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 请求权限
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.MODIFY_AUDIO_SETTINGS), 1)
        ADAudioManager.setRecordCallback(object : ADAudioCallback {
            override fun onError(errorCode: Int, errorMsg: String) {
                showToast("code:$errorCode msg:$errorMsg")
            }

            override fun onSaveFinish(wavFile: File) {
                this@MainActivity.wavFile = wavFile
                showToast("音频wav文件转换完成")
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1 && PackageManager.PERMISSION_DENIED in grantResults) {
            showToast("拒绝权限将无法使用本功能")
            finish()
        }
    }


    fun startRecord(view: View) {
       ADAudioManager.startRecordOnlySave("test")
    }

    fun stopRecord(view: View) {
        ADAudioManager.stopRecord()
    }



}