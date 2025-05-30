import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger
import javax.sound.sampled.*

/**
 * Класс для работы с микрофоном и получения аудиоданных через Flow
 */
class MicrophoneSharedFlow() {
    // Поток аудиоданных доступный извне
    private val _audioFlow = MutableSharedFlow<ByteArray>()

    private val logger = LoggerAssistant
    val audioFlow: SharedFlow<ByteArray> = _audioFlow.asSharedFlow()

    val format = AudioFormat(16000f, 16, 1, true, true)
    val info = DataLine.Info(TargetDataLine::class.java, format)

    fun run() {
        logger.info("Начало записи...")

        if (!AudioSystem.isLineSupported(info)) {
            logger.info("Линия не поддерживается")
            return
        }

        val line = AudioSystem.getLine(info) as TargetDataLine
        line.open(format)
        line.start()

        LoggerAssistant.info("Начало записи...")

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
        logger.info("Запись завершена.")
        logger.info("Длина записанного аудио: ${out.size()} байт")

        // Сохранение в WAV-файл
        val audioBytes = out.toByteArray()
        val bais = ByteArrayInputStream(audioBytes)
        val audioInputStream = AudioInputStream(bais, format, (audioBytes.size / format.frameSize).toLong())

        val wavFile = File("recorded_audio.wav")
        AudioSystem.write(audioInputStream, AudioFileFormat.Type.WAVE, wavFile)
        logger.info("Файл сохранен как ${wavFile.absolutePath}")
    }
}