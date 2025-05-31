import LoggerAssistant
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.ByteArrayInputStream
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.logging.Logger
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.sound.sampled.AudioFileFormat
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioInputStream
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.spi.AudioFileWriter

fun disableSSLVerification() {
    val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    })

    val sslContext = SSLContext.getInstance("TLS")
    sslContext.init(null, trustAllCerts, SecureRandom())
    SSLContext.setDefault(sslContext)
}

fun main() {

    disableSSLVerification()

    // Создаем Basic Auth ключ
    val authorizationKey =
        "ODkwNzBmOTYtZmI5MS00YjU5LTgzZWQtZDNkZTEyOTI1MWE2OmYyZThlOTU4LTQ5Y2QtNDczYi04Y2EyLTJiNmY4NmIzYTk4OA=="
    val scope = "SALUTE_SPEECH_PERS"

    val authManager = SpeechKitAuth(authorizationKey, scope)
    val accessToken = authManager.getAccessToken()

    val speechKitClient = SpeechKitClient(accessToken)

    val microphoneSgaredFlow = MicrophoneSharedFlow()

    CoroutineScope(Dispatchers.IO).launch {
        speechKitClient
            .recognizeFlow(
                microphoneSgaredFlow
                    .audioFlow
                    .onEach {
                        LoggerAssistant.info("Получены данные с микрофона")
                        //delay(4000)
                    }
            )
            .collect { result ->
                applyResult(result, authManager)
            }
    }

    microphoneSgaredFlow.run()

    runBlocking {
        delay(20000)
    }
}

    fun applyResult(result: RecognitionResult, authManager: SpeechKitAuth, ) {
        val logger = LoggerAssistant
        logger.info("Результат распознавания: $result")
    when (result) {
        is RecognitionResult.Transcription -> {
            logger.info("Текст: ${result.text}")
            if (result.isFinal) logger.info("ФИНАЛЬНЫЙ РЕЗУЛЬТАТ: ${result.text}")
        }

        is RecognitionResult.BackendInfo -> {
        }

        is RecognitionResult.Insight -> {
            logger.info("Insight: ${result.data}")
        }

        is RecognitionResult.VadInfo ->
            logger.info("Голосовая активность: ${if (result.hasVoice) "Есть голос" else "Нет голоса"}")

        is RecognitionResult.Error -> {
            logger.info("Ошибка: ${result.message}")
            if (result.message.contains("UNAUTHENTICATED")) {
                logger.info("Токен устарел, получаем новый...")
                authManager.refreshAccessToken()
            }
        }
    }
}
