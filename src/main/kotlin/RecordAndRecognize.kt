import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.sound.sampled.*

    /**
     * Запись звука с микрофона и потоковое распознавание
     */
    fun recordAndRecognize(): Flow<ByteArray> {
        val audioFormat = AudioFormat(16000f, 16, 1, true, false)
        // Создаем объект для захвата аудио
        val targetInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)
        if (!AudioSystem.isLineSupported(targetInfo)) {
           // logger.severe("Микрофон с указанным форматом не поддерживается")
            throw IllegalArgumentException("Микрофон с указанным форматом не поддерживается")
        }

        val microphone = AudioSystem.getLine(targetInfo) as TargetDataLine
        microphone.open(audioFormat)
        microphone.start()

            // Буфер для чтения аудиоданных
            val buffer = ByteArray(3200) // 100 мс аудио при 16кГц 16-бит
          //  isRecording = true

        return callbackFlow<ByteArray> {
            microphone.read(buffer, 0, buffer.size)

            trySend(buffer)

            // Останавливаем запись

            awaitClose {
                microphone.stop()
                microphone.close()
            }
        }
    }

