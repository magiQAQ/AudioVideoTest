package me.magi.media.utils

import android.util.Log

internal object ADLogUtil {
    private val TAG = this::class.simpleName!!

    private val LINE_SEP = System.getProperty("line.separator")
    private const val TOP_CORNER = "┌"
    private const val MIDDLE_CORNER = "├"
    private const val LEFT_BORDER = "│ "
    private const val BOTTOM_CORNER = "└"
    private const val SIDE_DIVIDER = "────────────────────────────────────────────────────────"
    private const val MIDDLE_DIVIDER = "┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄┄"
    private const val TOP_BORDER: String = TOP_CORNER + SIDE_DIVIDER + SIDE_DIVIDER
    private const val MIDDLE_BORDER: String = MIDDLE_CORNER + MIDDLE_DIVIDER + MIDDLE_DIVIDER
    private const val BOTTOM_BORDER: String = BOTTOM_CORNER + SIDE_DIVIDER + SIDE_DIVIDER

    fun d(vararg message: Any) {
        val content = createConsoleContent(*message)
        Log.d(TAG, content)
    }

    fun dTAG(tag: String = TAG, vararg message: Any) {
        val content = createConsoleContent(*message)
        Log.d(TAG, content)
    }

    private fun createConsoleContent(vararg message: Any): String {
        val builder = StringBuilder()
        builder.append(LINE_SEP).append(TOP_BORDER).append(LINE_SEP)
        for (i in message.indices) {
            if (i!=0) {
                builder.append(MIDDLE_BORDER).append(LINE_SEP)
            }
            builder.append(LEFT_BORDER).append(message[i].toString()).append(LINE_SEP)
        }
        builder.append(BOTTOM_BORDER).append(LINE_SEP)
        return builder.toString()
    }


}