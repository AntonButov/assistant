package microphoneSharedFlow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import tools.LoggerAssistant
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

class MicrophoneSharedFlow {
    // Поток аудиоданных доступный извне
    private val _audioFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 1)

    private val logger = LoggerAssistant
    val audioFlow: Flow<ByteArray> = _audioFlow.asSharedFlow()

    val format = AudioFormat(16000f, 16, 1, true, true)
    val info = DataLine.Info(TargetDataLine::class.java, format)

    suspend fun run() =
        withContext(Dispatchers.IO) {
            logger.info("Начало записи...")

            val mixers = AudioSystem.getMixerInfo()
            val availableLines = mutableListOf<TargetDataLine>()

            // Поиск доступных аудиолиний (микрофон и системный звук)
            for (mixerInfo in mixers) {
                try {
                    val mixer = AudioSystem.getMixer(mixerInfo)
                    logger.info("Проверяем микшер: ${mixerInfo.name}")

                    if (mixer.isLineSupported(info)) {
                        val line = mixer.getLine(info) as TargetDataLine
                        line.open(format)
                        line.start()
                        availableLines.add(line)
                        logger.info("Добавлена линия: ${mixerInfo.name}")
                    }
                } catch (e: Exception) {
                    logger.info("Не удалось открыть линию для ${mixerInfo.name}: ${e.message}")
                }
            }

            if (availableLines.isEmpty()) {
                logger.info("Не найдено поддерживаемых аудиолиний")
                return@withContext
            }

            logger.info("Найдено ${availableLines.size} аудиолиний")

            coroutineScope {
                // Запускаем захват для каждой доступной линии в отдельной корутине
                availableLines.forEach { line ->
                    launch {
                        captureAudio(line)
                    }
                }
            }

            // Закрываем все линии
            availableLines.forEach { line ->
                line.stop()
                line.close()
            }

            logger.info("Запись завершена.")
        }

    private suspend fun captureAudio(line: TargetDataLine) =
        withContext(Dispatchers.IO) {
            val stopTime = System.currentTimeMillis() + 15000

            while (System.currentTimeMillis() < stopTime) {
                try {
                    val buffer = ByteArray(10024)
                    val bytesRead = line.read(buffer, 0, buffer.size)

                    if (bytesRead > 0) {
                        val totalBytes = ByteArrayOutputStream()
                        totalBytes.write(buffer, 0, bytesRead)

                        val wavOutputStream = ByteArrayOutputStream()
                        AudioSystem.write(
                            AudioInputStream(
                                ByteArrayInputStream(totalBytes.toByteArray()),
                                format,
                                totalBytes.size().toLong() / format.frameSize,
                            ),
                            AudioFileFormat.Type.WAVE,
                            wavOutputStream,
                        )
                        _audioFlow.tryEmit(wavOutputStream.toByteArray())
                    }
                } catch (e: Exception) {
                    logger.info("Ошибка при захвате аудио: ${e.message}")
                    break
                }
            }
        }

    // Метод для получения информации о доступных микшерах
    fun getAvailableMixers(): List<String> {
        val mixers = AudioSystem.getMixerInfo()
        return mixers.map { "${it.name} - ${it.description}" }
    }

    // Метод для захвата только с определенного микшера
    suspend fun runWithSpecificMixer(mixerName: String) =
        withContext(Dispatchers.IO) {
            logger.info("Начало записи с микшера: $mixerName")

            val mixers = AudioSystem.getMixerInfo()
            val targetMixer = mixers.find { it.name.contains(mixerName, ignoreCase = true) }

            if (targetMixer == null) {
                logger.info("Микшер '$mixerName' не найден")
                return@withContext
            }

            try {
                val mixer = AudioSystem.getMixer(targetMixer)
                if (!mixer.isLineSupported(info)) {
                    logger.info("Микшер не поддерживает нужный формат")
                    return@withContext
                }

                val line = mixer.getLine(info) as TargetDataLine
                line.open(format)
                line.start()

                captureAudio(line)

                line.stop()
                line.close()
            } catch (e: Exception) {
                logger.info("Ошибка при работе с микшером: ${e.message}")
            }

            logger.info("Запись завершена.")
        }
}
