package com.magi.adlive.encode

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import com.magi.adlive.model.Force
import com.magi.adlive.util.ADLogUtil
import java.nio.ByteBuffer
import java.util.concurrent.ArrayBlockingQueue
import kotlin.math.min

abstract class BaseEncoder: EncodeCallback {

    companion object{
        var presentTimeUs = 0L
    }
    protected var TAG = "BaseEncoder"
    private val bufferInfo = MediaCodec.BufferInfo()
    private lateinit var handlerThread: HandlerThread
    protected var queue = ArrayBlockingQueue<Frame>(80)
    protected lateinit var codec: MediaCodec
    @Volatile
    protected var isRunning = false
    protected var isBufferMode = true
    protected var force = Force.FIRST_COMPATIBLE_FOUND
    private lateinit var callback: MediaCodec.Callback
    private var oldTimeStamp = 0L
    protected var shouldReset = true

    fun restart() {
        start(false)
        initCodec()
    }

    fun start() {
        if (presentTimeUs == 0L) {
            presentTimeUs = System.nanoTime() / 1000
        }
        start(true)
        initCodec()
    }

    private fun initCodec() {
        handlerThread = HandlerThread(TAG)
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        createAsyncCallback()
        codec.setCallback(callback, handler)
        codec.start()
        isRunning = true
    }

    abstract fun reset()

    abstract fun start(resetTs: Boolean)

    protected abstract fun stopImp()

    protected fun fixTimeStamp(info: MediaCodec.BufferInfo) {
        if (oldTimeStamp > info.presentationTimeUs) {
            info.presentationTimeUs = oldTimeStamp
        } else {
            oldTimeStamp = info.presentationTimeUs
        }
    }

    private fun reloadCodec() {
        if (shouldReset){
            ADLogUtil.logE(TAG, "Encoder crashed, trying to recover it")
            reset()
        }
    }

    fun stop(resetTs: Boolean = true) {
        if (resetTs) {
            presentTimeUs = 0
        }
        isRunning = false
        stopImp()
        handlerThread.looper?.thread?.interrupt()
        handlerThread.looper?.quit()
        handlerThread.quit()
        try { codec.flush() } catch (ignored: IllegalStateException) {}
        try {
            handlerThread.looper?.thread?.join(500)
        } catch (ignored: Exception) {}
        queue.clear()
        queue = ArrayBlockingQueue(80)
        try {
            codec.stop()
            codec.release()
        } catch (e: Exception) {}
        oldTimeStamp = 0L
    }

    protected abstract fun chooseEncoder(mime: String): MediaCodecInfo?

    @Throws(InterruptedException::class)
    protected abstract fun getInputFrame(): Frame?

    protected abstract fun checkBuffer(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo)

    protected abstract fun sendBuffer(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo)

    @Throws(IllegalStateException::class)
    private fun processInput(byteBuffer: ByteBuffer, mediaCodec: MediaCodec, inBufferIndex: Int) {
        try {
            var frame = getInputFrame()
            while (frame == null) frame = getInputFrame()
            byteBuffer.clear()
            val size = min(frame.size, byteBuffer.remaining())
            byteBuffer.put(frame.buffer, frame.offset, size)
            val pts = System.nanoTime() / 1000 - presentTimeUs
            mediaCodec.queueInputBuffer(inBufferIndex, 0, size, pts, 0)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
        } catch (e: IndexOutOfBoundsException){
            ADLogUtil.logE(TAG, "Encode error", e)
        }
    }

    @Throws(IllegalStateException::class)
    private fun processOutput(byteBuffer: ByteBuffer, mediaCodec: MediaCodec
                              , outBufferIndex: Int, bufferInfo: MediaCodec.BufferInfo) {
        checkBuffer(byteBuffer, bufferInfo)
        sendBuffer(byteBuffer, bufferInfo)
        mediaCodec.releaseOutputBuffer(outBufferIndex, false)
    }

    override fun inputAvailable(mediaCodec: MediaCodec, inBufferIndex: Int) {
        mediaCodec.getInputBuffer(inBufferIndex)?.let {
            processInput(it, mediaCodec, inBufferIndex)
        }
    }

    override fun outputAvailable(mediaCodec: MediaCodec, outBufferIndex: Int, bufferInfo: MediaCodec.BufferInfo) {
        mediaCodec.getInputBuffer(outBufferIndex)?.let {
            processOutput(it, mediaCodec, outBufferIndex, bufferInfo)
        }
    }

    private fun createAsyncCallback() {
        callback = object :MediaCodec.Callback() {
            override fun onInputBufferAvailable(codec: MediaCodec, index: Int) {
                try {
                    inputAvailable(codec, index)
                } catch (e: IllegalStateException) {
                    ADLogUtil.logE(TAG, "Encoding error", e)
                    reloadCodec()
                }
            }

            override fun onOutputBufferAvailable(codec: MediaCodec, index: Int, info: MediaCodec.BufferInfo) {
                try {
                    outputAvailable(codec, index, info)
                } catch (e: IllegalStateException) {
                    ADLogUtil.logE(TAG, "Encoding error", e)
                    reloadCodec()
                }
            }

            override fun onError(codec: MediaCodec, e: MediaCodec.CodecException) {
                ADLogUtil.logE(TAG, "Error", e)
            }

            override fun onOutputFormatChanged(codec: MediaCodec, format: MediaFormat) {
                formatChanged(codec, format)
            }
        }
    }
}