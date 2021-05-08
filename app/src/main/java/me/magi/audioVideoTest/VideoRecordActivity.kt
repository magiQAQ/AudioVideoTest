package me.magi.audioVideoTest

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.TextureView
import android.view.View
import android.widget.Button
import me.magi.media.video.ADCameraConstant
import me.magi.media.video.ADVideoManager

class VideoRecordActivity : AppCompatActivity() {

    private lateinit var textureView: TextureView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_record)
        textureView = findViewById(R.id.textureView)

        ADVideoManager.setTextureView(textureView)
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
        }
    }

    fun startPreview(view: View) {
        ADVideoManager.startPreview(ADCameraConstant.CAMERA_FACING_BACK)
    }

    fun stopPreview(view: View) {
        ADVideoManager.stopPreview()
    }

    private var enableFlash = false
        set(value) {
            field = value
            ADVideoManager.setFlashState(value)
        }

    @SuppressLint("SetTextI18n")
    fun openCloseFlash(view: View) {
        enableFlash = !enableFlash
        view as Button
        view.text = "${if (enableFlash) "关闭" else "打开"}闪光灯"
    }

    private var enableAutoFocus = true
        set(value) {
            field = value
            ADVideoManager.setAutoFocusState(value)
        }

    @SuppressLint("SetTextI18n")
    fun openCloseAutoFocus(view: View) {
        enableAutoFocus = !enableAutoFocus
        view as Button
        view.text = "${if (enableAutoFocus) "关闭" else "开启"}自动对焦"
    }
}