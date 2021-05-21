package me.magi.media.utils

import me.magi.media.widget.ADRenderView
import java.lang.ref.WeakReference

internal class ADRenderWrapper(renderView: ADRenderView) {

    private val renderViewHolder: WeakReference<ADRenderView> = WeakReference(renderView)



}