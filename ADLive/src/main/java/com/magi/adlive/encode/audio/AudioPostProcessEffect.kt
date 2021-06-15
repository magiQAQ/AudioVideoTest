package com.magi.adlive.encode.audio

import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AutomaticGainControl
import android.media.audiofx.NoiseSuppressor
import java.util.concurrent.Semaphore

// 音频硬件处理效果
class AudioPostProcessEffect(private val microphoneId: Int) {

    private val TAG = "AudioPostProcessEffect"

    private var semaphore = Semaphore(0)

    // 回声消除器
    private var acousticEchoCanceler: AcousticEchoCanceler? = null
    // 自动增益
    private var automaticGainControl: AutomaticGainControl? = null
    // 噪音抑制
    private var noiseSuppressor: NoiseSuppressor? = null


    fun enableEchoCancel(enable: Boolean) {
        semaphore.acquireUninterruptibly()
        if (enable && acousticEchoCanceler == null) {
            acousticEchoCanceler = AcousticEchoCanceler.create(microphoneId)
        }
        acousticEchoCanceler?.enabled = enable
        semaphore.release()
    }

    fun enableAutoGain(enable: Boolean) {
        semaphore.acquireUninterruptibly()
        if (enable && automaticGainControl == null) {
            automaticGainControl = AutomaticGainControl.create(microphoneId)
        }
        automaticGainControl?.enabled = enable
        semaphore.release()
    }

    fun enableNoiseSuppress(enable: Boolean) {
        semaphore.acquireUninterruptibly()
        if (enable && noiseSuppressor == null) {
            noiseSuppressor = NoiseSuppressor.create(microphoneId)
        }
        noiseSuppressor?.enabled = enable
        semaphore.release()
    }

    fun release() {
        semaphore.acquireUninterruptibly()

        acousticEchoCanceler?.enabled = false
        automaticGainControl?.enabled = false
        noiseSuppressor?.enabled = false

        acousticEchoCanceler?.release()
        acousticEchoCanceler = null
        automaticGainControl?.release()
        automaticGainControl = null
        noiseSuppressor?.release()
        noiseSuppressor = null
        semaphore.release()
    }

}