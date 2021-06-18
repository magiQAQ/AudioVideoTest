package com.magi.adlive.encode.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.HandlerThread
import com.magi.adlive.encode.Frame
import com.magi.adlive.util.ADLogUtil
import java.nio.ByteBuffer
import java.util.concurrent.Semaphore

class ADMicrophoneController(private val getMicrophoneData: GetMicrophoneData) {
    private val TAG = "MicrophoneManager"
    private var BUFFER_SIZE = 0
    private var audioRecord: AudioRecord? = null
    private var pcmBuffer: ByteBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
    private var pcmBufferMuted = ByteArray(BUFFER_SIZE)
    var isRunning = false
        private set
    var isCreated = false
        private set
    private var sampleRate = 44100
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT
    private var channel = AudioFormat.CHANNEL_IN_STEREO
    var isMuted = false
    private lateinit var audioPostProcessEffect: AudioPostProcessEffect
    private lateinit var handlerThread: HandlerThread

    private val semaphore = Semaphore(1)

    fun createMicrophone(audioSource: Int, sampleRate: Int, channelCount: Int, echoCancel: Boolean, autoGain: Boolean,
    noiseSuppress: Boolean): Boolean {
        semaphore.acquireUninterruptibly()
        this.sampleRate = sampleRate
        this.channel = if (channelCount == 2) AudioFormat.CHANNEL_IN_STEREO else AudioFormat.CHANNEL_IN_MONO
        try {
            audioRecord?.release()
            audioRecord = AudioRecord(audioSource, sampleRate, channel, audioFormat, getPcmBufferSize())
            audioPostProcessEffect = AudioPostProcessEffect(audioRecord!!.audioSessionId)
            audioPostProcessEffect.enableEchoCancel(echoCancel)
            audioPostProcessEffect.enableAutoGain(autoGain)
            audioPostProcessEffect.enableNoiseSuppress(noiseSuppress)
            if (audioRecord!!.state != AudioRecord.STATE_INITIALIZED) {
                throw IllegalStateException("Some parameters specified is not valid")
            }
            ADLogUtil.logD(TAG, "Microphone create, $sampleRate HZ, $channelCount channel")
            isCreated = true
        } catch (e: IllegalStateException) {
            ADLogUtil.logE(TAG, e.message?:"", e)
        }
        semaphore.release()
        return isCreated
    }

    private fun getPcmBufferSize(): Int {
        BUFFER_SIZE = AudioRecord.getMinBufferSize(sampleRate, channel, audioFormat)
        pcmBuffer = ByteBuffer.allocateDirect(BUFFER_SIZE)
        pcmBufferMuted = ByteArray(BUFFER_SIZE)
        return BUFFER_SIZE * 5
    }

    fun start() {
        semaphore.acquireUninterruptibly()
        if (audioRecord!=null) {
            audioRecord!!.startRecording()
            isRunning = true
            ADLogUtil.logD(TAG, "Microphone started")
        } else {
            ADLogUtil.logE(TAG, "Error starting, please call createMicrophone() before start()")
        }
        handlerThread = HandlerThread(TAG)
        handlerThread.start()
        val handler = Handler(handlerThread.looper)
        handler.post {
            while (isRunning) {
                val frame = read()
                if (frame != null) {
                    getMicrophoneData.inputPCMData(frame)
                }
            }

        }
        semaphore.release()
    }

    fun stop() {
        isRunning = false
        isCreated = false
        handlerThread.quitSafely()
        audioRecord?.let {
            it.setRecordPositionUpdateListener(null)
            it.stop()
            it.release()
        }
        audioRecord = null
        audioPostProcessEffect.release()
    }

    private fun read(): Frame? {
        pcmBuffer.rewind()
        val size = audioRecord?.read(pcmBuffer, pcmBuffer.remaining())?:0
        if (size < 0) return null
        return Frame(if (isMuted) pcmBufferMuted else pcmBuffer.array(), if (isMuted) 0 else pcmBuffer.arrayOffset(), size)
    }

    fun getMaxInputSize(): Int {
        return BUFFER_SIZE
    }
}