package tech.antonbutov

import SpeechKitAuth
import SpeechKitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.logging.Level
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

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
    // Создаем Basic Auth ключ (Base64 от "clientId:clientSecret")
    val authorizationKey = "ODkwNzBmOTYtZmI5MS00YjU5LTgzZWQtZDNkZTEyOTI1MWE2OmYyZThlOTU4LTQ5Y2QtNDczYi04Y2EyLTJiNmY4NmIzYTk4OA=="

    val scope = "SALUTE_SPEECH_PERS"
    val audioFilePath = "outm.mp3"

    // Create a logger
    val logger = org.slf4j.LoggerFactory.getLogger("Main")

        // Создаем менеджер аутентификации с использованием Ktor
        val authManager = SpeechKitAuth(authorizationKey, scope)

        // Получаем токен доступа
        val accessToken = authManager.getAccessToken()

        // Используем токен для распознавания речи
        SpeechKitClient(
            accessKey = accessToken,
            scope = scope,
        ).use { client ->
            CoroutineScope(Dispatchers.Unconfined).launch {
                client
                    .recognizeAsFlow(File(audioFilePath))
                    .catch { e ->
                        logger.error("Необработанная ошибка в Flow", e)
                    }
                    .collect { result ->
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
                            logger.error("Ошибка: ${result.message}")
                            if (result.message.contains("UNAUTHENTICATED")) {
                                logger.info("Токен устарел, получаем новый...")
                                authManager.refreshAccessToken()
                            }
                        }
                    }
                }
            }
        }
}
