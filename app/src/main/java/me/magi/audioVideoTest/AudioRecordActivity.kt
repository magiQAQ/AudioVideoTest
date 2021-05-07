package me.magi.audioVideoTest

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import me.magi.media.audio.ADAudioCallback
import me.magi.media.audio.ADAudioManager
import me.magi.media.utils.showToast
import java.io.File

class AudioRecordActivity : AppCompatActivity() {
    private var wavFile: File? = null

    companion object{
        private const val PERMISSION_REQUEST_CODE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_audio_record)

        // 检查权限
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_DENIED) {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), PERMISSION_REQUEST_CODE)
        }

        ADAudioManager.setRecordCallback(object : ADAudioCallback {
            override fun onError(errorCode: Int, errorMsg: String) {
                showToast("code:$errorCode msg:$errorMsg")
            }

            override fun onSaveFinish(wavFile: File) {
                this@AudioRecordActivity.wavFile = wavFile
                showToast("音频wav文件转换完成")
            }

            override fun onPlayTime(currentTime: Int, totalTime: Int) {
                
            }
        })
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE && PackageManager.PERMISSION_DENIED in grantResults) {
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

    fun startPlay(view: View) {
        if (wavFile == null) {
            showToast("请先录制")
        } else {
            ADAudioManager.startPlay(wavFile!!)
        }
    }

    fun stopPlay(view: View) {
        ADAudioManager.stopPlay()
    }

}