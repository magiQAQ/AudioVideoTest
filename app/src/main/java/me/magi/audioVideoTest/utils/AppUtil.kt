package me.magi.audioVideoTest.utils

import android.app.Application

private lateinit var mApplication: Application

fun init(application: Application) {
    mApplication = application
}

fun getApp() = mApplication