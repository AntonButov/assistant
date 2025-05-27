import TODO.Salutespeech
import TODO.SmartSpeechGrpc
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import javax.sound.sampled.*
import kotlin.concurrent.thread

    /**
     * Запись звука с микрофона и потоковое распознавание
     */
    fun recordAndRecognize(
        recordDurationSeconds: Int = 0, // 0 = запись до нажатия Enter
        languageCode: String = "ru-RU",
    ) {
        val audioFormat = AudioFormat(16000f, 16, 1, true, false)
        // Создаем объект для захвата аудио
        val targetInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)
        if (!AudioSystem.isLineSupported(targetInfo)) {
           // logger.severe("Микрофон с указанным форматом не поддерживается")
            return
        }

        val microphone = AudioSystem.getLine(targetInfo) as TargetDataLine
        microphone.open(audioFormat)
        microphone.start()

            // Буфер для чтения аудиоданных
            val buffer = ByteArray(3200) // 100 мс аудио при 16кГц 16-бит
          //  isRecording = true

        val bytesRead = microphone.read(buffer, 0, buffer.size)
            // Запускаем таймер для автоматического завершения, если указана длительность

            // Останавливаем запись
       //     microphone.stop()
       //     microphone.close()

    }

