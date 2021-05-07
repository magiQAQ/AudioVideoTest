package me.magi.media.utils

import android.app.Application
import android.hardware.camera2.CameraManager
import android.util.Log
import android.widget.Toast
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private lateinit var mApplication: Application
private const val TAG = "AdMedia"

fun init(application: Application) {
    mApplication = application
}

internal fun getApp() = mApplication

fun showToast(content: String){
    Toast.makeText(getApp(), content, Toast.LENGTH_SHORT).show()
}

object ADAppUtil{

    val cameraManager: CameraManager by lazy { mApplication.getSystemService(CameraManager::class.java) }

    val executor: Executor by lazy { Executors.newCachedThreadPool() }

}



