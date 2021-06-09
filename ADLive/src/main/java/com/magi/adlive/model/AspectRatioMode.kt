package com.magi.adlive.model

enum class AspectRatioMode(val id: Int) {
    Adjust(0),
    Fill(1),
    AdjustRotate(2),
    FillRotate(3);

    fun fromId(id: Int): AspectRatioMode {
        for (mode in values()) {
            if (mode.id == id) return mode
        }
        throw IllegalArgumentException("no this mode")
    }
}