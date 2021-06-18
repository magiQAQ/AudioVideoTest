package me.magi.audioVideoTest

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Size
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.magi.adlive.ADLiveView
import com.magi.adlive.librtmp.utils.ConnectCheckerRtmp
import com.magi.adlive.live.ADLivePusher
import com.magi.adlive.model.Facing

class VideoRecordActivity : AppCompatActivity(), ConnectCheckerRtmp{

    private lateinit var liveView: ADLiveView
    private lateinit var size: Size
    private lateinit var livePusher: ADLivePusher
    private val streamUrl = "rtmp://a.1029.lcps.aodianyun.com/live/1"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_record)
        liveView = findViewById(R.id.liveView)
        livePusher = ADLivePusher(liveView, this)

        val permissionList = ArrayList<String>()
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.CAMERA)
        }
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissionList.add(Manifest.permission.RECORD_AUDIO)
        }
        if (permissionList.isNotEmpty()) {
            requestPermissions(permissionList.toTypedArray(), 101)
        }

    }

    fun startPreview(view: View) {
        livePusher.startPreview(Facing.FRONT, 1920, 1080, 30)
    }

    fun stopPreview(view: View) {
        livePusher.stopPreview()
    }

    fun startStream(view: View) {
        if (livePusher.prepareVideo(1920, 1080, 30, 6000 * 1024, 3)
            && livePusher.prepareAudio()) {
            livePusher.startStreaming(streamUrl)
        }
    }

    fun stopStream(view: View) {
        livePusher.stopStreaming()
    }

//    private var enableFlash = false
//        set(value) {
//            field = value
//            ADVideoManager.setFlashState(value)
//        }

    @SuppressLint("SetTextI18n")
    fun openCloseFlash(view: View) {
//        enableFlash = !enableFlash
//        view as Button
//        view.text = "${if (enableFlash) "关闭" else "打开"}闪光灯"
    }

//    private var enableAutoFocus = true
//        set(value) {
//            field = value
//            ADVideoManager.setAutoFocusState(value)
//        }

    @SuppressLint("SetTextI18n")
    fun openCloseAutoFocus(view: View) {
//        enableAutoFocus = !enableAutoFocus
//        view as Button
//        view.text = "${if (enableAutoFocus) "关闭" else "开启"}自动对焦"
    }

    override fun onConnectionStartedRtmp(rtmpUrl: String) {

    }

    override fun onConnectionSuccessRtmp() {
        runOnUiThread { Toast.makeText(this, "connect success", Toast.LENGTH_SHORT).show() }
    }

    override fun onConnectionFailedRtmp(reason: String) {
        runOnUiThread {
            Toast.makeText(this, "connect failed: $reason", Toast.LENGTH_SHORT).show()
            livePusher.stopStreaming()
        }
    }

    override fun onNewBitrateRtmp(bitrate: Long) {}

    override fun onDisconnectRtmp() {
        runOnUiThread {
            Toast.makeText(this, "disconnected", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAuthErrorRtmp() {
        runOnUiThread {
            Toast.makeText(this, "auth error", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onAuthSuccessRtmp() {
        runOnUiThread {
            Toast.makeText(this, "auth success", Toast.LENGTH_SHORT).show()
        }
    }
}