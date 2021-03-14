package me.magi.audioVideoTest

import android.app.Application
import me.magi.audioVideoTest.utils.init

class MyApplication: Application() {
    override fun onCreate() {
        super.onCreate()
        init(this)
    }
}