package me.magi.audioVideoTest

import android.app.Application
import me.magi.media.utils.ADAppUtil.init

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        init(this)
    }
}