import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.RandomAccessFile
import java.util.logging.Logger
import javax.sound.sampled.AudioFormat
import java.util.concurrent.atomic.AtomicInteger

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
        writeWavHeader(rafWav, 0, audioFormat)
    }

    // Используем RandomAccessFile для записи в WAV-файл
    // Записываем заглушку заголовка WAV (44 байта)
    // Счетчик записанных байт
    val totalBytesWritten = AtomicInteger(0)

    fun finish() {
        writeWavHeader(rafWav, totalBytesWritten.get(), audioFormat)
        rafWav.close()
        LoggerAssistant.info("Запись в файл завершена, сохранено ${totalBytesWritten.get() / 1024} KB")
    }

    fun writeBytes(
        audioChunk: ByteArray,
    ) {
        val position = 44 + totalBytesWritten.get()
        rafWav.seek(position.toLong())
        rafWav.write(audioChunk)
        // Увеличиваем счетчик
        totalBytesWritten.addAndGet(audioChunk.size)
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