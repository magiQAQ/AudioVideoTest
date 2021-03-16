package me.magi.audioVideoTest

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity :AppCompatActivity(){

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    fun testAudioRecord(view: View) {
        startActivity(Intent(this, AudioRecordActivity::class.java))
    }

    fun testVideoRecord(view: View) {
        startActivity(Intent(this, VideoRecordActivity::class.java))
    }



}