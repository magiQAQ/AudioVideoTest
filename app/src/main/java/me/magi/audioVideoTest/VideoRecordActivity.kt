package me.magi.audioVideoTest

import android.Manifest
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.TextureView
import android.view.View
import me.magi.media.video.ADCameraConstant
import me.magi.media.video.ADVideoManager

class VideoRecordActivity : AppCompatActivity() {

    private var textureView: TextureView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_record)
        textureView = findViewById(R.id.textureView)

        ADVideoManager.setTextureView(textureView!!)
        if (checkSelfPermission(Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CAMERA), 101)
        }
    }

    fun startPreview(view: View) {
        ADVideoManager.startPreview(ADCameraConstant.CAMERA_FACING_BACK)
    }

    fun stopPreview(view: View) {
        ADVideoManager.stopPreview()
    }
}