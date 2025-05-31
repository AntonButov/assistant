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

        val buffer = ByteArray(200024)
        var bytesRead: Int

        val stopTime = System.currentTimeMillis()
        //while (System.currentTimeMillis() < stopTime) {
            bytesRead = line.read(buffer, 0, buffer.size)
        //}

        line.stop()
        line.close()

        val out = ByteArrayOutputStream()
        out.write(buffer, 0, bytesRead)
        val audioFile = File("recorded_audio.wav")
        AudioSystem.write(
            AudioInputStream(ByteArrayInputStream(out.toByteArray()), format, out.size().toLong()),
            AudioFileFormat.Type.WAVE, audioFile
        )

        val audioBytes = audioFile.readBytes()
        _audioFlow.tryEmit(audioBytes)

        logger.info("Запись завершена.")
    }
}