package me.magi.audioVideoTest

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.TextureView

class VideoRecordActivity : AppCompatActivity() {

    private var textureView: TextureView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_record)
        textureView = findViewById(R.id.textureView)

    }
}