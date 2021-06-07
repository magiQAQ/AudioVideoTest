package com.magi.adlive.util

import android.util.Log

internal object ADLogUtil {

    internal const val TAG = "ADLogUtil"

    fun logE(tag: String = TAG, message: String) {
        Log.e(TAG, message)
    }

    fun logD(tag: String = TAG, message: String) {
        Log.d(TAG, message)
    }

}