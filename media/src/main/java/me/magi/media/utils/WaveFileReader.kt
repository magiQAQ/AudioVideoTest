package me.magi.media.utils

import java.io.BufferedInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.lang.Exception

class WaveFileReader(file: File) {
    private var file: File? = null

    // 获取数据
    // 数据是一个二维数组，[n][m]代表第n个声道的第m个采样值
    var data: Array<IntArray>? = null
        private set

    // 获取数据长度，也就是一共采样多少个
    var dataLen = 0
        private set
    private var chunkdescriptor: String? = null
    private var chunksize: Long = 0
    private var waveflag: String? = null
    private var fmtubchunk: String? = null
    private var subchunk1size: Long = 0
    private var audioformat = 0

    // 获取声道个数，1代表单声道 2代表立体声
    var numChannels = 0
        private set

    // 获取采样率
    var sampleRate: Long = 0
        private set
    private var byterate: Long = 0
    private var blockalign = 0

    // 获取每个采样的编码长度，8bit或者16bit
    var bitPerSample = 0
        private set
    private var datasubchunk: String? = null
    private var subchunk2size: Long = 0
    private var fis: FileInputStream? = null
    private var bis: BufferedInputStream? = null

    // 判断是否创建wav读取器成功
    var isSuccess = false
        private set

    private fun initReader(file: File) {
        this.file = file
        try {
            fis = FileInputStream(this.file)
            bis = BufferedInputStream(fis)
            chunkdescriptor = readString(lenchunkdescriptor)
            require(chunkdescriptor!!.endsWith("RIFF")) { "RIFF miss, ${file.name} is not a wave file." }
            chunksize = readLong()
            waveflag = readString(lenwaveflag)
            require(waveflag!!.endsWith("WAVE")) { "WAVE miss, ${file.name} is not a wave file." }
            fmtubchunk = readString(lenfmtubchunk)
            require(fmtubchunk!!.endsWith("fmt ")) { "fmt miss, ${file.name} is not a wave file." }
            subchunk1size = readLong()
            audioformat = readInt()
            numChannels = readInt()
            sampleRate = readLong()
            byterate = readLong()
            blockalign = readInt()
            bitPerSample = readInt()
            datasubchunk = readString(lendatasubchunk)
            require(datasubchunk!!.endsWith("data")) { "data miss, ${file.name} is not a wave file." }
            subchunk2size = readLong()
            dataLen = (subchunk2size / (bitPerSample / 8) / numChannels).toInt()
            data = Array(numChannels) { IntArray(dataLen) }
            for (i in 0 until dataLen) {
                for (n in 0 until numChannels) {
                    if (bitPerSample == 8) {
                        data!![n][i] = bis!!.read()
                    } else if (bitPerSample == 16) {
                        data!![n][i] = readInt()
                    }
                }
            }
            isSuccess = true
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            try {
                if (bis != null) bis!!.close()
                if (fis != null) fis!!.close()
            } catch (e1: Exception) {
                e1.printStackTrace()
            }
        }
    }

    private fun readString(len: Int): String {
        val buf = ByteArray(len)
        try {
            if (bis!!.read(buf) != len) throw IOException("no more data!!!")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return String(buf)
    }

    private fun readInt(): Int {
        val buf = ByteArray(2)
        var res = 0
        try {
            if (bis!!.read(buf) != 2) throw IOException("no more data!!!")
            res = buf[0].toInt() and 0x000000FF or (buf[1].toInt() shl 8)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return res
    }

    private fun readLong(): Long {
        var res: Long = 0
        try {
            val l = LongArray(4)
            for (i in 0..3) {
                l[i] = bis!!.read().toLong()
                if (l[i] == -1L) {
                    throw IOException("no more data!!!")
                }
            }
            res = l[0] or (l[1] shl 8) or (l[2] shl 16) or (l[3] shl 24)
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return res
    }

    private fun readBytes(len: Int): ByteArray {
        val buf = ByteArray(len)
        try {
            if (bis!!.read(buf) != len) throw IOException("no more data!!!")
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return buf
    }

    companion object {
        private const val lenchunkdescriptor = 4
        private const val lenchunksize = 4
        private const val lenwaveflag = 4
        private const val lenfmtubchunk = 4
        private const val lensubchunk1size = 4
        private const val lenaudioformat = 2
        private const val lennumchannels = 2
        private const val lensamplerate = 2
        private const val lenbyterate = 4
        private const val lenblockling = 2
        private const val lenbitspersample = 2
        private const val lendatasubchunk = 4
        private const val lensubchunk2size = 4
    }

    init {
        initReader(file)
    }
}