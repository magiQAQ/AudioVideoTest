@file:JvmName("ADLiveBase")
package me.magi.media.utils

import android.app.Application

private lateinit var mApplication: Application

fun init(application: Application) {
    mApplication = application
}

internal fun getApp() = mApplication



