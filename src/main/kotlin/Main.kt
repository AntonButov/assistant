import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.logging.Logger
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.sound.sampled.AudioFormat

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
    val authorizationKey = "ODkwNzBmOTYtZmI5MS00YjU5LTgzZWQtZDNkZTEyOTI1MWE2OmYyZThlOTU4LTQ5Y2QtNDczYi04Y2EyLTJiNmY4NmIzYTk4OA=="
    val scope = "SALUTE_SPEECH_PERS"

    // Создаем логгер
    val logger = Logger.getLogger("SpeechRecognition")
    logger.info("Запуск приложения распознавания речи")

    // Определяем файл для записи
    val recordedFile = File("")
        // Создаем менеджер аутентификации с использованием Ktor
        val authManager = SpeechKitAuth(authorizationKey, scope)

        // Получаем токен доступа
        val accessToken = authManager.getAccessToken()

        // Используем токен для распознавания речи
        SpeechKitClient(
            accessKey = accessToken,
            scope = scope,
        ).also { client ->
            runBlocking {
                client
                   // .recognizeFile(File(audioFilePath))
                    .recognizeMicrophone()
                    .catch { e ->
                        logger.info("Необработанная ошибка в Flow $e")
                    }
                    .collect { result ->
                    logger.info("result = $result")
                    applyResult(result, logger, authManager)
                }
            }
        }
}

private fun applyResult(result: RecognitionResult, logger: Logger, authManager: SpeechKitAuth, ) {
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