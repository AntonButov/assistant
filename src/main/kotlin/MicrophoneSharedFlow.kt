import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.util.concurrent.atomic.AtomicBoolean
import java.util.logging.Level
import java.util.logging.Logger
import javax.sound.sampled.*

/**
 * Класс для работы с микрофоном и получения аудиоданных через Flow
 */
class MicrophoneSharedFlow() {
    // Поток аудиоданных доступный извне
    private val _audioFlow = MutableStateFlow<ByteArray>(byteArrayOf())

    private val logger = LoggerAssistant
    val audioFlow: SharedFlow<ByteArray> = _audioFlow.asSharedFlow()

    // Внутренние переменные для управления состоянием
    private var microphone: TargetDataLine? = null
    private var recordingJob: Job? = null
    private val isRunning = AtomicBoolean(false)

    /**
     * Начать запись с микрофона
     * @return true если запись успешно начата, false в противном случае
     */
    fun start(): Boolean {
        if (isRunning.getAndSet(true)) {
            logger.info("Запись уже идет")
            return false
        }

        try {
            LoggerAssistant.info("Настройка микрофона")

            // Настраиваем формат аудио
            val audioFormat = AudioFormat(16000f, 16, 1, true, false)
            val targetInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)

            // Проверяем поддержку микрофона
            if (!AudioSystem.isLineSupported(targetInfo)) {
                logger.info("Микрофон с указанным форматом не поддерживается")
                isRunning.set(false)
                return false
            }

            // Инициализируем микрофон
            microphone = AudioSystem.getLine(targetInfo) as TargetDataLine
            microphone?.open(audioFormat)
            microphone?.start()

            // Запускаем корутину для сбора аудиоданных
         //   recordingJob = scope.launch(Dispatchers.IO) {
                logger.info("Начало записи с микрофона")

                val buffer = ByteArray(1600) // 100мс аудио при 16кГц, 16бит, моно
                var totalBytesRead = 0

                try {
                    while (isRunning.get()) {
                        microphone?.let { mic ->
                            val bytesRead = mic.read(buffer, 0, buffer.size)
                            if (bytesRead > 0) {
                                val audioChunk = buffer.copyOfRange(0, bytesRead)
                                _audioFlow.update { audioChunk }

                                totalBytesRead += bytesRead
                                if (totalBytesRead % 16000 == 0) { // Примерно каждую секунду
                                    logger.info("Собрано с микрофона: ${totalBytesRead / 1024} KB")
                                }
                            }
                            //delay(5) // Небольшая задержка для предотвращения перегрузки CPU
                        } ?: break // Если микрофон null, выходим из цикла
                    }
                } catch (e: CancellationException) {
                    logger.info("Корутина сбора аудио отменена")
                    throw e
                } catch (e: Exception) {
                    logger.info("Ошибка при записи с микрофона: ${e.message}")
                    isRunning.set(false)
                }
           // }

            return true
        } catch (e: Exception) {
            logger.info("Ошибка при инициализации микрофона: ${e.message}")
            stop() // Очищаем ресурсы в случае ошибки
            return false
        }
    }

    /**
     * Остановить запись с микрофона
     */
    fun stop() {
        if (!isRunning.getAndSet(false)) {
            return
        }

        // Отменяем корутину
        recordingJob?.cancel()
        recordingJob = null

        // Освобождаем ресурсы микрофона
        try {
            microphone?.stop()
            microphone?.close()
            microphone = null
            logger.info("Микрофон успешно остановлен")
        } catch (e: Exception) {
            logger.info("Ошибка при остановке микрофона: ${e.message}")
        }
    }
}