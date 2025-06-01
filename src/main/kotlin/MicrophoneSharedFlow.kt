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
    private val _audioFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 1)

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

        val stopTime = System.currentTimeMillis() + 15000
        while (System.currentTimeMillis() < stopTime) {
            val buffer = ByteArray(50024)
            val bytesRead = line.read(buffer, 0, buffer.size)
            val totalBytes = ByteArrayOutputStream()
            totalBytes.write(buffer, 0, bytesRead)

            val wavOutputStream = ByteArrayOutputStream()
            AudioSystem.write(
                AudioInputStream(
                    ByteArrayInputStream(totalBytes.toByteArray()),
                    format,
                    totalBytes.size().toLong() / format.frameSize
                ),
                AudioFileFormat.Type.WAVE,
                wavOutputStream
            )
            _audioFlow.tryEmit(wavOutputStream.toByteArray())
        }

        line.stop()
        line.close()

        logger.info("Запись завершена.")
    }
}