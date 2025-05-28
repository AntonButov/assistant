import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.logging.Logger
import javax.sound.sampled.*

fun createMicrophoneSharedFlow(
    scope: CoroutineScope,
    logger: Logger
): Pair<SharedFlow<ByteArray>, kotlinx.coroutines.Job> {

    // Создаем SharedFlow с буфером для обеспечения надежности
    val audioFlow = MutableSharedFlow<ByteArray>(
        replay = 5,    // Хранить последние 5 фрагментов для новых подписчиков
        extraBufferCapacity = 10  // Дополнительный буфер для предотвращения блокировок
    )

    logger.info("Инициализация SharedFlow микрофона")

    // Запускаем сбор аудио в отдельной корутине
    val job = scope.launch(Dispatchers.IO) {
        try {
            val audioFormat = AudioFormat(16000f, 16, 1, true, false)
            val targetInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)

            logger.info("Проверка поддержки микрофона")

            if (!AudioSystem.isLineSupported(targetInfo)) {
                logger.severe("Микрофон с указанным форматом не поддерживается")
                throw IllegalStateException("Микрофон с указанным форматом не поддерживается")
            }

            // Настраиваем микрофон
            val microphone = AudioSystem.getLine(targetInfo) as TargetDataLine
            microphone.open(audioFormat)
            microphone.start()

            // Буфер для чтения аудио (размер для 100 мс аудио при 16кГц, 16-бит, моно)
            val buffer = ByteArray(1600)
            var totalBytesRead = 0

            logger.info("Начинаем сбор аудио с микрофона")

            try {
                // Цикл сбора аудио
                while (true) {
                    val bytesRead = microphone.read(buffer, 0, buffer.size)
                    if (bytesRead > 0) {
                        val audioChunk = buffer.copyOfRange(0, bytesRead)

                        // Отправляем в SharedFlow
                        audioFlow.emit(audioChunk)

                        totalBytesRead += bytesRead
                        if (totalBytesRead % 16000 == 0) {  // Примерно каждую секунду
                            logger.info("Собрано с микрофона: ${totalBytesRead / 1024} KB")
                        }

                        // Небольшая задержка для предотвращения перегрузки процессора
                        delay(10)
                    }
                }
            } finally {
                // Освобождаем ресурсы микрофона при завершении
                logger.info("Закрытие микрофона")
                microphone.stop()
                microphone.close()
            }
        } catch (e: Exception) {
            logger.severe("Ошибка при работе с микрофоном: ${e.message}")
            throw e
        }
    }

    // Возвращаем SharedFlow и job для управления сбором
    return audioFlow.asSharedFlow() to job
}