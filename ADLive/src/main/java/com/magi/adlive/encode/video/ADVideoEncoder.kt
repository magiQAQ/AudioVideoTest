package com.magi.adlive.encode.video

import android.graphics.ImageFormat
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Bundle
import android.view.Surface
import com.magi.adlive.encode.*
import com.magi.adlive.util.ADFpsLimiter
import com.magi.adlive.util.ADLogUtil
import com.magi.adlive.util.YUVUtil
import java.nio.ByteBuffer

class ADVideoEncoder(private val getVideoData: GetVideoData): BaseEncoder(), GetCameraData {

    private var spsPpsSett = false
    private var forceKey = false

    var inputSurface: Surface? = null
        private set
    var width = 1280
        private set
    var height = 720
        private set
    var fps = 30
    var bitrate = 3000 * 1024
        private set
    var rotation = 90
        private set
    private var iFrameInterval = 2
    var type = H264_MIME

    private var fpsLimiter = ADFpsLimiter()
    private var formatVideoEncoder: FormatVideoEncoder = FormatVideoEncoder.YUV420Dynamical
    private var avcProfile = -1
    private var avcProfileLevel = -1


    init {
        TAG = "VideoEncoder"
    }

    fun prepareVideoEncoder(width: Int, height: Int, fps: Int, bitrate: Int, rotation: Int,
                            iFrameInterval: Int, formatVideoEncoder: FormatVideoEncoder,
                            avcProfile: Int = -1, avcProfileLevel: Int = -1): Boolean {
        this.width = width
        this.height = height
        this.fps = fps
        this.bitrate = bitrate
        this.rotation = rotation
        this.formatVideoEncoder = formatVideoEncoder
        this.avcProfile = avcProfile
        this.avcProfileLevel = avcProfileLevel
        isBufferMode = true
        val encoderInfo = chooseEncoder(type)
        try {
            if (encoderInfo != null) {
                ADLogUtil.logD(TAG, "Encoder selected ${encoderInfo.name}")
                codec = MediaCodec.createByCodecName(encoderInfo.name)
//                if (this.formatVideoEncoder == FormatVideoEncoder.YUV420Dynamical) {
//                    val encoder = chooseColorDynamically(encoderInfo)
//                    if (encoder == null) {
//                        ADLogUtil.logE(TAG, "YUV420 dynamical choose failed")
//                        return false
//                    }
//                    this.formatVideoEncoder = encoder
//                }
            } else {
                ADLogUtil.logE(TAG, "Valid encoder not found")
                return false
            }
            val videoFormat = if (rotation == 90 || rotation == 270) {
                ADLogUtil.logD(TAG, "Prepare video info: ${encoderInfo.name}, $height x $width")
                MediaFormat.createVideoFormat(type, height, width)
            } else {
                ADLogUtil.logD(TAG, "Prepare video info: ${encoderInfo.name}, $width x $height")
                MediaFormat.createVideoFormat(type, width, height)
            }
            videoFormat.setInteger(MediaFormat.KEY_COLOR_FORMAT, formatVideoEncoder.getFormatCodec())
            videoFormat.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 0)
            videoFormat.setInteger(MediaFormat.KEY_BIT_RATE, bitrate)
            videoFormat.setInteger(MediaFormat.KEY_FRAME_RATE, fps)
            videoFormat.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, iFrameInterval)
            if (isCBRModeSupported(encoderInfo)) {
                videoFormat.setInteger(MediaFormat.KEY_BITRATE_MODE, MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
            }
            videoFormat.setInteger(MediaFormat.KEY_PROFILE, avcProfile)
            videoFormat.setInteger(MediaFormat.KEY_LEVEL, avcProfileLevel)
            codec.configure(videoFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
            isRunning = false
            if (formatVideoEncoder == FormatVideoEncoder.SURFACE) {
                isBufferMode = false
                inputSurface = codec.createInputSurface()
            }
            ADLogUtil.logD(TAG, "prepared")
            return true
        } catch (e: Exception) {
            ADLogUtil.logE(TAG, "create VideoEncoder failed", e)
            stop()
            return false
        }
    }

    override fun chooseEncoder(mime: String): MediaCodecInfo? {
        val mediaCodecInfoList = getAllHardwareEncoders(mime)
        ADLogUtil.logD(TAG, "Found encoder $mime count ${mediaCodecInfoList.size}")
        val cbrPriority = ArrayList<MediaCodecInfo>()
        for (mci in mediaCodecInfoList) {
            if (isCBRModeSupported(mci)) {
                cbrPriority.add(mci)
            }
        }
        mediaCodecInfoList.removeAll(cbrPriority)
        mediaCodecInfoList.addAll(cbrPriority)
        for (mci in mediaCodecInfoList) {
            ADLogUtil.logD(TAG, "Encoder ${mci.name}")
            val codecCapabilities = mci.getCapabilitiesForType(mime)
            for (format in codecCapabilities.colorFormats) {
                ADLogUtil.logD(TAG, "format support: $format")
                if (formatVideoEncoder == FormatVideoEncoder.SURFACE) {
                    if (format == FormatVideoEncoder.SURFACE.getFormatCodec()) return mci
                } else {
                    //check if encoder support any yuv420 color
                    if (format == FormatVideoEncoder.YUV420PLANAR.getFormatCodec()
                        || format == FormatVideoEncoder.YUV420SEMIPLANAR.getFormatCodec()) {
                        return mci
                    }
                }
            }
        }
        return null
    }

    private fun isCBRModeSupported(mediaCodecInfo: MediaCodecInfo): Boolean {
        val codecCapabilities = mediaCodecInfo.getCapabilitiesForType(type)
        val encoderCapabilities = codecCapabilities.encoderCapabilities
        return encoderCapabilities.isBitrateModeSupported(MediaCodecInfo.EncoderCapabilities.BITRATE_MODE_CBR)
    }

    private fun chooseColorDynamically(mediaCodecInfo: MediaCodecInfo): FormatVideoEncoder? {
        for (format in mediaCodecInfo.getCapabilitiesForType(type).colorFormats) {
            if (format == FormatVideoEncoder.YUV420PLANAR.getFormatCodec()) {
                return FormatVideoEncoder.YUV420PLANAR
            } else if (format == FormatVideoEncoder.YUV420SEMIPLANAR.getFormatCodec()) {
                return FormatVideoEncoder.YUV420SEMIPLANAR
            }
        }
        return null
    }

    override fun start(resetTs: Boolean) {
        forceKey = false
        shouldReset = resetTs
        spsPpsSett = false
        if (resetTs) {
            fpsLimiter.setFps(fps)
        }
        if (formatVideoEncoder != FormatVideoEncoder.SURFACE) {
            YUVUtil.preAllocateBuffers(width * height * 3 / 2)
        }
        ADLogUtil.logD(TAG, "started")
    }

    override fun stopImp() {
        spsPpsSett = false
        inputSurface?.release()
        inputSurface = null
        ADLogUtil.logD(TAG, "stopped")
    }

    override fun reset() {
        stop(false)
        prepareVideoEncoder(width, height, fps, bitrate, rotation, iFrameInterval, formatVideoEncoder,
        avcProfile, avcProfileLevel)
        restart()
    }

    fun setVideoBitrateOnFly(bitrate: Int) {
        if (isRunning) {
            this.bitrate
            val bundle = Bundle().apply {
                putInt(MediaCodec.PARAMETER_KEY_VIDEO_BITRATE, bitrate)
            }
            try {
                codec.setParameters(bundle)
            } catch (e: IllegalStateException) {
                ADLogUtil.logE(TAG, "encoder need be running", e)
            }
        }
    }

    fun requestKeyframe() {
        if (isRunning) {
            if (spsPpsSett) {
                val bundle = Bundle().apply {
                    putInt(MediaCodec.PARAMETER_KEY_REQUEST_SYNC_FRAME, 0)
                }
                try {
                    codec.setParameters(bundle)
                } catch (e: IllegalStateException) {
                    ADLogUtil.logE(TAG, "encoder need be running", e)
                }
            } else {
                //You need wait until encoder generate first frame
                forceKey = true
            }
        }
    }

    override fun inputYUVData(frame: Frame) {
        if (isRunning && !queue.offer(frame)) {
            ADLogUtil.logD(TAG, "frame discarded")
        }
    }

    private fun sendSPSandPPS(mediaFormat: MediaFormat) {
        when (type) {
            H265_MIME -> {
                val byteBufferList: List<ByteBuffer> = extractVpsSpsPpsFromH265(mediaFormat.getByteBuffer("csd-0"))
                if (byteBufferList.isNotEmpty()) {
                    getVideoData.onSpsPpsVps(byteBufferList[1], byteBufferList[2], byteBufferList[0])
                }
            }
            H264_MIME -> {
                val sps = mediaFormat.getByteBuffer("csd-0")
                val pps = mediaFormat.getByteBuffer("csd-1")
                getVideoData.onSpsPpsVps(sps!!, pps!!, null)
            }
        }
    }

    // 检查视频编码器中是否有sps信息和pps信息
    private fun decodeSpsPpsFromBuffer(outputBuffer: ByteBuffer, length: Int): Pair<ByteBuffer, ByteBuffer>? {
        val csd = ByteArray(length)
        outputBuffer.get(csd, 0 , length)
        var i = 0
        var spsIndex = -1
        var ppsIndex = -1
        while (i < length - 4) {
            if (csd[i] == 0.toByte() && csd[i + 1] == 0.toByte()
                && csd[i + 2] == 0.toByte() && csd[i + 3] == 1.toByte()) {
                if (spsIndex == -1) {
                    spsIndex = i
                } else {
                    ppsIndex = i
                    break
                }
            }
            i++
        }
        // sps信息和pps信息一定是一起有的
        if (spsIndex != -1 && ppsIndex != -1) {
            val sps = ByteArray(ppsIndex)
            System.arraycopy(csd, spsIndex, sps, 0, ppsIndex)
            val pps = ByteArray(length - ppsIndex)
            System.arraycopy(csd, ppsIndex, pps, 0, length - ppsIndex)
            return Pair(ByteBuffer.wrap(sps), ByteBuffer.wrap(pps))
        }
        return null
    }

    private fun extractVpsSpsPpsFromH265(csd0byteBuffer: ByteBuffer?): MutableList<ByteBuffer> {
        val byteBufferList: MutableList<ByteBuffer> = ArrayList()
        if (csd0byteBuffer == null) return byteBufferList
        var vpsPosition = -1
        var spsPosition = -1
        var ppsPosition = -1
        var contBufferInitiation = 0
        val length = csd0byteBuffer.remaining()
        val csdArray = ByteArray(length)
        csd0byteBuffer[csdArray, 0, length]
        for (i in csdArray.indices) {
            if (contBufferInitiation == 3 && csdArray[i] == 1.toByte()) {
                if (vpsPosition == -1) {
                    vpsPosition = i - 3
                } else if (spsPosition == -1) {
                    spsPosition = i - 3
                } else {
                    ppsPosition = i - 3
                }
            }
            if (csdArray[i] == 0.toByte()) {
                contBufferInitiation++
            } else {
                contBufferInitiation = 0
            }
        }
        val vps = ByteArray(spsPosition)
        val sps = ByteArray(ppsPosition - spsPosition)
        val pps = ByteArray(csdArray.size - ppsPosition)
        for (i in csdArray.indices) {
            if (i < spsPosition) {
                vps[i] = csdArray[i]
            } else if (i < ppsPosition) {
                sps[i - spsPosition] = csdArray[i]
            } else {
                pps[i - ppsPosition] = csdArray[i]
            }
        }
        byteBufferList.add(ByteBuffer.wrap(vps))
        byteBufferList.add(ByteBuffer.wrap(sps))
        byteBufferList.add(ByteBuffer.wrap(pps))
        return byteBufferList
    }

    override fun getInputFrame(): Frame? {
        val frame = queue.take() ?: return null
        if (fpsLimiter.limitFPS()) return getInputFrame()
        var buffer = frame.buffer
        val isYV12 = frame.format == ImageFormat.YV12

        var orientation = if (frame.isFlip) frame.orientation + 180 else frame.orientation
        if (orientation >= 360) orientation -= 360
        buffer = if (isYV12) YUVUtil.rotateYV12(buffer, width, height, orientation)!!
        else YUVUtil.rotateNV21(buffer, width, height, orientation)!!
        buffer = if (isYV12) YUVUtil.YV12toYUV420byColor(buffer, width, height, formatVideoEncoder)!!
        else YUVUtil.NV21toYUV420byColor(buffer, width, height, formatVideoEncoder)!!
        frame.buffer = buffer
        return frame
    }

    override fun formatChanged(mediaCodec: MediaCodec, mediaFormat: MediaFormat) {
        getVideoData.onVideoFormat(mediaFormat)
        sendSPSandPPS(mediaFormat)
        spsPpsSett = true
    }

    override fun checkBuffer(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        if (forceKey) {
            forceKey = false
            requestKeyframe()
        }
        fixTimeStamp(bufferInfo)
        if (!spsPpsSett) {
            when (type) {
                H264_MIME -> {
                    ADLogUtil.logD(TAG, "formatChanged not called, doing manual sps/pps extraction...")
                    val buffers = decodeSpsPpsFromBuffer(byteBuffer.duplicate(), bufferInfo.size)
                    if (buffers != null) {
                        ADLogUtil.logD(TAG, "manual sps/pps extraction success")
                        getVideoData.onSpsPpsVps(buffers.first, buffers.second, null)
                        spsPpsSett = true
                    } else {
                        ADLogUtil.logE(TAG, "manual sps/pps extraction failed")
                    }
                }
                H265_MIME -> {
                    ADLogUtil.logD(TAG, "formatChanged not called, doing manual vps/sps/pps extraction...")
                    val byteBufferList = extractVpsSpsPpsFromH265(byteBuffer)
                    if (byteBufferList.size == 3) {
                        ADLogUtil.logD(TAG, "manual vps/sps/pps extraction success")
                        getVideoData.onSpsPpsVps(byteBufferList[0], byteBufferList[1], byteBufferList[2])
                        spsPpsSett = true
                    } else {
                        ADLogUtil.logE(TAG, "manual vps/sps/pps extraction failed")
                    }
                }
            }
            if (formatVideoEncoder == FormatVideoEncoder.SURFACE) {
                bufferInfo.presentationTimeUs = System.nanoTime() / 1000 - presentTimeUs
            }
        }
    }

    override fun sendBuffer(byteBuffer: ByteBuffer, bufferInfo: MediaCodec.BufferInfo) {
        getVideoData.getVideoData(byteBuffer, bufferInfo)
    }
}