import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.logging.Logger
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

    // Создаем Basic Auth ключ
    val authorizationKey = "ODkwNzBmOTYtZmI5MS00YjU5LTgzZWQtZDNkZTEyOTI1MWE2OmYyZThlOTU4LTQ5Y2QtNDczYi04Y2EyLTJiNmY4NmIzYTk4OA=="
    val scope = "SALUTE_SPEECH_PERS"

    // Создаем логгер
    val logger = Logger.getLogger("SpeechRecognition")
    logger.info("Запуск приложения распознавания речи")

    // Файл для записи
    val recordedFile = File("recorded_audio.wav")

    runBlocking {
        try {
            // Получаем токен авторизации
            val authManager = SpeechKitAuth(authorizationKey, scope)
            val accessToken = authManager.getAccessToken()

            // Создаем менеджер микрофона
            val microphoneManager = MicrophoneSharedFlow(
                scope = this,
                logger = logger
            )

            // Запускаем запись в файл
            val fileWriteJob = launch {
              //  writeAudioToFile(
               //     microphoneManager.audioFlow,
              //      recordedFile,
              //      microphoneManager.getAudioFormat(),
              //      logger
            //    )
            }

            // Запускаем распознавание речи
            val recognitionJob = launch {
                SpeechKitClient(
                    accessKey = accessToken,
                    scope = scope
                ).use { client ->
                    microphoneManager.audioFlow
                        .catch { e -> logger.severe("Ошибка при обработке аудиопотока: ${e.message}") }
                        .collect { audioChunk ->
                            // Отправляем чанки на распознавание
                           // client.recognize(audioChunk)
                           //     .collect { result ->
                           //         applyResult(result, logger, authManager)
                           //     }
                        }
                }
            }

            // Запускаем микрофон
            if (microphoneManager.start()) {
                logger.info("Запись с микрофона успешно начата")

                // Записываем и распознаем 20 секунд
                delay(20000)

                // Останавливаем запись
                microphoneManager.stop()
                logger.info("Запись с микрофона остановлена")

                // Отменяем корутины записи и распознавания
                fileWriteJob.cancelAndJoin()
                recognitionJob.cancelAndJoin()

                logger.info("Все задачи завершены")
            } else {
                logger.severe("Не удалось запустить запись с микрофона")
            }
        } catch (e: Exception) {
            logger.severe("Ошибка выполнения: ${e.message}")
            e.printStackTrace()
        }
    }

    logger.info("Программа завершена")
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