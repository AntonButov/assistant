package tech.antonbutov

import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import javax.sound.sampled.AudioInputStream
import java.io.File

fun main() {
    val sampleRate = 16000f
    val sampleSizeInBits = 16
    val channels = 1
    val signed = true
    val bigEndian = false

    val format = AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian)
    val info = DataLine.Info(TargetDataLine::class.java, format)

    if (!AudioSystem.isLineSupported(info)) {
        println("Line not supported")
        return
    }

    val line = AudioSystem.getLine(info) as TargetDataLine
    line.open(format)
    line.start()

    val wavFile = File("output.wav")
    println("Recording...")

    // Создаем AudioInputStream из TargetDataLine
    val audioStream = AudioInputStream(line)

    // Запись в течение 5 секунд
    val recordTime = 5 * sampleRate.toInt() // 5 секунд

    // Записываем аудио в файл
    AudioSystem.write(audioStream, AudioFileFormat.Type.WAVE, wavFile)

    println("Finished recording.")
    line.stop()
    line.close()
}
