package com.magi.adlive.util

import android.util.Log

internal object ADLogUtil {

    internal const val TAG = "ADLogUtil"

    fun logE(tag: String = TAG, message: String, e: Exception? = null) {
        if (e == null) {
            Log.e(tag, message)
        } else {
            Log.e(tag, message, e)
        }
    }

    fun logD(tag: String = TAG, message: String) {
        Log.d(tag, message)
    }

}