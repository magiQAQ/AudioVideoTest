package me.magi.audioVideoTest

import android.app.Application
import com.magi.adlive.ADLiveBase

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
//        init(this)
        ADLiveBase.init(this)
    }
}