package me.magi.audioVideoTest.utils

import android.content.Context
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException

const val FILE_DIR_NAME = "AUDIO_TEST"

@Throws(IOException::class)
fun pcmFile2WavFile(pcmFile: File, wavFile: File, sampleRate: Int, channels: Int, bufferSize: Int) {
    if (!pcmFile.exists()) throw IOException("pcm文件不存在")
    if (wavFile.exists()) {
        wavFile.delete()
    }
    wavFile.createNewFile()
    val fis = FileInputStream(pcmFile)
    val fos = FileOutputStream(wavFile)
    val header = getWaveFileHeader(pcmFile.length(), sampleRate, channels)
    fos.write(header)
    val array = ByteArray(bufferSize)
    while (fis.read(array)!=-1) {
        fos.write(array)
    }
    fos.flush()
    fis.close()
    fos.close()
}

/**
 * 加入wav文件头 , 采样深度默认16bit
 */
private fun getWaveFileHeader(
    totalPcmLen: Long,
    sampleRate: Int,
    channels: Int,
): ByteArray {
    // 音频采样速率
    val bytePerSecond = sampleRate.toLong() * channels * 16 / 8
    val totalDataLen = totalPcmLen + 36
    val header = ByteArray(44)
    header[0] = 'R'.toByte() // RIFF
    header[1] = 'I'.toByte()
    header[2] = 'F'.toByte()
    header[3] = 'F'.toByte()
    header[4] = (totalDataLen and 0xff).toByte() // 数据大小 低位在前高位在后
    header[5] = (totalDataLen shr 8 and 0xff).toByte()
    header[6] = (totalDataLen shr 16 and 0xff).toByte()
    header[7] = (totalDataLen shr 24 and 0xff).toByte()
    header[8] = 'W'.toByte() //WAVE
    header[9] = 'A'.toByte()
    header[10] = 'V'.toByte()
    header[11] = 'E'.toByte()
    // FMT Chunk
    header[12] = 'f'.toByte() // 'fmt '标记符占据四个字节
    header[13] = 'm'.toByte()
    header[14] = 't'.toByte()
    header[15] = ' '.toByte() //过渡字节
    // 数据大小
    header[16] = 16.toByte() // 4 bytes: 'fmt '的大小
    header[17] = 0.toByte()
    header[18] = 0.toByte()
    header[19] = 0.toByte()
    //编码方式 1 为PCM编码格式
    header[20] = 1.toByte() // format = 1
    header[21] = 0.toByte()
    //通道数
    header[22] = channels.toByte()
    header[23] = 0.toByte()
    //采样率，每个通道的播放速度
    header[24] = (sampleRate and 0xff).toByte()
    header[25] = (sampleRate shr 8 and 0xff).toByte()
    header[26] = (sampleRate shr 16 and 0xff).toByte()
    header[27] = (sampleRate shr 24 and 0xff).toByte()
    //音频数据传送速率,采样率*通道数*采样深度/8
    header[28] = (bytePerSecond and 0xff).toByte()
    header[29] = (bytePerSecond shr 8 and 0xff).toByte()
    header[30] = (bytePerSecond shr 16 and 0xff).toByte()
    header[31] = (bytePerSecond shr 24 and 0xff).toByte()
    // 确定系统一次要处理多少个这样字节的数据，确定缓冲区，通道数*采样位数
    header[32] = (channels * 16 / 8).toByte()
    header[33] = 0.toByte()
    //每个样本的数据位数
    header[34] = 16.toByte()
    header[35] = 0.toByte()
    //Data chunk
    header[36] = 'd'.toByte() //data
    header[37] = 'a'.toByte()
    header[38] = 't'.toByte()
    header[39] = 'a'.toByte()
    header[40] = (totalPcmLen and 0xff).toByte()
    header[41] = (totalPcmLen shr 8 and 0xff).toByte()
    header[42] = (totalPcmLen shr 16 and 0xff).toByte()
    header[43] = (totalPcmLen shr 24 and 0xff).toByte()
    return header
}

@Throws(IOException::class)
fun getAudioDir(context: Context): File{
    val dir = context.getExternalFilesDir(FILE_DIR_NAME) ?: throw IOException("外部存储当前不可用")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    return dir
}