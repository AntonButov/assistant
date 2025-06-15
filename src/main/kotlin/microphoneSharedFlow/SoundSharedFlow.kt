package microphoneSharedFlow

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable.join
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.isActive
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
import kotlin.coroutines.cancellation.CancellationException

class SoundSharedFlow {
    // Поток аудиоданных доступный извне
    private val _audioFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 1)

    private val logger = LoggerAssistant
    val audioFlow: Flow<ByteArray> = _audioFlow.asSharedFlow()

    val format = AudioFormat(16000f, 16, 1, true, true)
    val info = DataLine.Info(TargetDataLine::class.java, format)

    private var recordingJob: Job? = null
    val availableLines = mutableListOf<TargetDataLine>()

    suspend fun run() =
        withContext(Dispatchers.IO) {
            recordingJob = coroutineContext[Job]

            val mixers = AudioSystem.getMixerInfo()

            // Поиск доступных аудиолиний (микрофон и системный звук)
            for (mixerInfo in mixers) {
                try {
                    val mixer = AudioSystem.getMixer(mixerInfo)
                    //logger.info("Проверяем микшер: ${mixerInfo.name}")

                    if (mixer.isLineSupported(info)) {
                        val line = mixer.getLine(info) as TargetDataLine
                        line.open(format)
                        line.start()
                        availableLines.add(line)
                        //logger.info("Добавлена линия: ${mixerInfo.name}")
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

            // сюда не доходит ???

        }

    fun stop() {
        recordingJob?.cancel()
        availableLines.forEach { line ->
            line.stop()
            line.close()
        }

        logger.info("Чтение остановлено.")
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private suspend fun captureAudio(line: TargetDataLine) =
        withContext(Dispatchers.IO) {

            while (coroutineContext.isActive) {
                try {
                    val buffer = ByteArray(1000000)
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
}
