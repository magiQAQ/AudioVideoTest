package me.magi.media.utils

import android.app.Application
import android.widget.Toast

private lateinit var mApplication: Application

fun init(application: Application) {
    mApplication = application
}

fun getApp() = mApplication

fun showToast(content: String){
    Toast.makeText(getApp(), content, Toast.LENGTH_SHORT).show()
}