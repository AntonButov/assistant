package tech.antonbutov.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine

class MicrophoneSharedFlow
    @Inject
    constructor() {
        // Поток аудиоданных доступный извне
        private val _audioFlow = MutableSharedFlow<ByteArray>(extraBufferCapacity = 1)

        private val logger = LoggerAssistant
        val audioFlow: Flow<ByteArray> = _audioFlow.asSharedFlow()

        val format = AudioFormat(16000f, 16, 1, true, true)
        val info = DataLine.Info(TargetDataLine::class.java, format)

        suspend fun run() =
            withContext(Dispatchers.IO) {
                logger.info("Начало записи...")

                if (!AudioSystem.isLineSupported(info)) {
                    logger.info("Линия не поддерживается")
                    return@withContext
                }

                val line = AudioSystem.getLine(info) as TargetDataLine
                line.open(format)
                line.start()

                LoggerAssistant.info("Начало записи...")

                val stopTime = System.currentTimeMillis() + 15000
                while (System.currentTimeMillis() < stopTime) {
                    val buffer = ByteArray(10024)
                    val bytesRead = line.read(buffer, 0, buffer.size)
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

                line.stop()
                line.close()

                logger.info("Запись завершена.")
            }
    }
