import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import java.util.logging.Logger
import javax.sound.sampled.*

/**
 * Creates a flow that emits audio data from the microphone
 */
fun microphoneFlow(logger: Logger): Flow<ByteArray> {
    logger.info("Starting microphone flow")

    return callbackFlow {
        val audioFormat = AudioFormat(16000f, 16, 1, true, false)
        val targetInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)

        logger.info("Checking if microphone is supported")

        if (!AudioSystem.isLineSupported(targetInfo)) {
            close(IllegalStateException("Микрофон с указанным форматом не поддерживается"))
            return@callbackFlow
        }

        val microphone = AudioSystem.getLine(targetInfo) as TargetDataLine
        microphone.open(audioFormat)
        microphone.start()

        val buffer = ByteArray(10000) // 100 ms of audio at 16kHz 16-bit

        logger.info("Start cycle")

        (1..10).forEach {
            val bytesRead = microphone.read(buffer, 0, buffer.size)
            if (bytesRead > 0) {
                val audioChunk = buffer.copyOfRange(0, bytesRead)
                logger.info("Sent audio chunk: $bytesRead bytes")
                trySend(audioChunk)
                // logger.severe("Микрофон с указанным форматом не поддерживается")
                // Буфер для чтения аудиоданных
            }
            delay(2000)
        }

        awaitClose {
            microphone.stop()
            microphone.close()
        }
    }.flowOn(Dispatchers.IO)
}
