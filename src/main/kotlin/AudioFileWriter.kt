import LoggerAssistant
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.RandomAccessFile
import java.util.logging.Logger
import javax.sound.sampled.AudioFormat
import java.util.concurrent.atomic.AtomicInteger
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

/**
 * Записывает аудио из SharedFlow в WAV-файл
 */
class WriterAudio(
    outputFile: File,
) {
    val rafWav = RandomAccessFile(outputFile, "rw")
    val audioFormat = AudioFormat(16000f, 16, 1, true, false)

    init {
        LoggerAssistant.info("Начинаем запись аудио в файл ${outputFile.absolutePath}")
        // Подготавливаем файл для записи
        outputFile.parentFile?.mkdirs()
        if (outputFile.exists()) outputFile.delete()
        outputFile.createNewFile()
        writeWavHeader(rafWav, 0, audioFormat)
    }

    // Используем RandomAccessFile для записи в WAV-файл
    // Записываем заглушку заголовка WAV (44 байта)
    // Счетчик записанных байт
    var totalBytesWritten = 0

    fun finish() {
        writeWavHeader(rafWav, totalBytesWritten, audioFormat)
        rafWav.close()
        LoggerAssistant.info("Запись в файл завершена, сохранено ${totalBytesWritten / 1024} KB")
    }

    fun writeBytes(
        audioChunk: ByteArray,
    ) {
        LoggerAssistant.info("Записываем ${audioChunk.size} байт")
        val position = 44 + totalBytesWritten
        rafWav.seek(position.toLong())
        rafWav.write(audioChunk)
        // Увеличиваем счетчик
        totalBytesWritten += audioChunk.size
        LoggerAssistant.info("Записано ${totalBytesWritten / 1024} KB")
    }

    // Функция для записи WAV-заголовка
    private fun writeWavHeader(file: RandomAccessFile, audioDataLength: Int, audioFormat: AudioFormat) {
        file.seek(0)

        // RIFF header
        file.writeBytes("RIFF") // ChunkID
        file.writeInt(Integer.reverseBytes(36 + audioDataLength)) // ChunkSize
        file.writeBytes("WAVE") // Format

        // fmt subchunk
        file.writeBytes("fmt ") // Subchunk1ID
        file.writeInt(Integer.reverseBytes(16)) // Subchunk1Size (16 for PCM)
        file.writeShort(java.lang.Short.reverseBytes(1.toShort()).toInt()) // AudioFormat (1 for PCM)
        file.writeShort(java.lang.Short.reverseBytes(audioFormat.channels.toShort()).toInt()) // NumChannels
        file.writeInt(Integer.reverseBytes(audioFormat.sampleRate.toInt())) // SampleRate

        val byteRate = (audioFormat.sampleRate * audioFormat.channels * audioFormat.sampleSizeInBits / 8).toInt()
        file.writeInt(Integer.reverseBytes(byteRate)) // ByteRate

        val blockAlign = (audioFormat.channels * audioFormat.sampleSizeInBits / 8).toShort()
        file.writeShort(java.lang.Short.reverseBytes(blockAlign).toInt()) // BlockAlign

        // Convert sampleSizeInBits to Short before calling reverseBytes
        val bitsPerSample = audioFormat.sampleSizeInBits.toShort()
        file.writeShort(java.lang.Short.reverseBytes(bitsPerSample).toInt()) // BitsPerSample

        // data subchunk
        file.writeBytes("data") // Subchunk2ID
        file.writeInt(Integer.reverseBytes(audioDataLength)) // Subchunk2Size
    }
}

// Остальные вспомогательные функции остаются без изменений


fun main() {
    val format = AudioFormat(16000f, 16, 1, true, true)
    val info = DataLine.Info(TargetDataLine::class.java, format)

    if (!AudioSystem.isLineSupported(info)) {
        println("Линия не поддерживается")
        return
    }

    val line = AudioSystem.getLine(info) as TargetDataLine
    line.open(format)
    line.start()

    println("Начало записи...")

    val out = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    var bytesRead: Int

    val stopTime = System.currentTimeMillis() + 15000
    while (System.currentTimeMillis() < stopTime) {
        bytesRead = line.read(buffer, 0, buffer.size)
        out.write(buffer, 0, bytesRead)
    }

    line.stop()
    line.close()
    LoggerAssistant.info("Запись завершена.")
    LoggerAssistant.info("Длина записанного аудио: ${out.size()} байт")

    // Сохранение в WAV-файл
    val audioBytes = out.toByteArray()
    val bais = ByteArrayInputStream(audioBytes)
    val audioInputStream = AudioInputStream(bais, format, (audioBytes.size / format.frameSize).toLong())

    val wavFile = File("recorded_audio.wav")
    AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavFile)
    LoggerAssistant.info("Файл сохранен как ${wavFile.absolutePath}")
}
