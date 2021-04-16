package me.magi.media.utils

import android.app.Application
import android.hardware.camera2.CameraManager
import android.widget.Toast

object ADAppUtil {
    private lateinit var mApplication: Application

    fun init(application: Application) {
        mApplication = application
    }

    fun getApp() = mApplication

    fun showToast(content: String){
        Toast.makeText(getApp(), content, Toast.LENGTH_SHORT).show()
    }

    val cameraManager: CameraManager by lazy { mApplication.getSystemService(CameraManager::class.java) }
}

