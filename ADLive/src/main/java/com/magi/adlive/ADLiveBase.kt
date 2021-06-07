package com.magi.adlive

import android.app.Application

object ADLiveBase {

    internal lateinit var application: Application

    @JvmStatic
    fun init(application: Application) {
        this.application = application
    }

//    internal fun getApplication(): Application = application

}