package me.magi.media.widget

import android.content.Context
import android.util.AttributeSet
import android.util.Size
import android.view.TextureView
import android.view.View

internal class AspectTextureView: TextureView {
    constructor(context: Context): super(context)
    constructor(context: Context, attrs: AttributeSet): super(context, attrs)

    companion object{
        //拉伸模式，改变显示内容的比例
        internal const val MODE_FIT_XY = 0
        //黑边填充模式，保持显示内容的比例，比例不一致时，使用黑边填充
        internal const val MODE_INSIDE = 1
        //裁剪模式，保持显示内容的比例，比例不一致时，裁剪多余的部分
        internal const val MODE_OUTSIDE = 2
    }

    private var aspectMode = MODE_INSIDE
    private var aspectSize = Size(9, 16)

    internal fun setAspectRatio(mode: Int, targetRatio: Size) {
        if (mode !in arrayOf(MODE_FIT_XY, MODE_INSIDE, MODE_OUTSIDE)) {
            throw IllegalArgumentException("illegal mode")
        }
        if (aspectSize.width <= 0 || aspectSize.height <= 0) {
            throw IllegalArgumentException("illegal size")
        }
        if (aspectMode != mode || aspectSize.width != targetRatio.width * aspectSize.height / targetRatio.height) {
            aspectMode = mode
            aspectSize = targetRatio
            requestLayout()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        var viewWidth = MeasureSpec.getSize(widthMeasureSpec)
        var viewHeight = MeasureSpec.getSize(heightMeasureSpec)
        val viewAspectRatio = viewWidth.toDouble() / viewHeight
        val targetRatio = aspectSize.width.toDouble() / aspectSize.height
        when (aspectMode) {
            MODE_INSIDE -> {
                if (targetRatio > viewAspectRatio) {
                    viewHeight = (viewWidth / targetRatio).toInt()
                } else {
                    viewWidth = (viewHeight * targetRatio).toInt()
                }
            }
            MODE_OUTSIDE -> {
                if (targetRatio > viewAspectRatio) {
                    viewWidth = (viewHeight * targetRatio).toInt()
                } else {
                    viewHeight = (viewWidth / targetRatio).toInt()
                }
            }
        }
        val finalWidthSpec = MeasureSpec.makeMeasureSpec(viewWidth, MeasureSpec.EXACTLY)
        val finalHeightSpec = MeasureSpec.makeMeasureSpec(viewHeight, MeasureSpec.EXACTLY)
        super.onMeasure(finalWidthSpec, finalHeightSpec)
    }

    override fun layout(l: Int, t: Int, r: Int, b: Int) {
        val p = parent as View?
        var left = l
        var top = t
        var right = r
        var bottom = b
        if (p != null) {
            val pw = p.measuredWidth
            val ph = p.measuredHeight
            val w = measuredWidth
            val h = measuredHeight
            left = (pw - w) / 2
            top = (ph - h) / 2
            right += left
            bottom += top
        }
        super.layout(left, top, right, bottom)
    }

}