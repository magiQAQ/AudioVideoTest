package me.magi.media.widget

import android.content.Context
import android.util.AttributeSet
import me.magi.media.utils.ADRenderWrapper

class ADRenderView: GLTextureView {

    private val glRender = ADRenderWrapper(this)

    constructor(context: Context): super(context)

    constructor(context: Context, attrs: AttributeSet): super(context, attrs)

    init {
        setEGLContextClientVersion(2)


    }

}