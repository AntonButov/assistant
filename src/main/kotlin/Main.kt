package tech.antonbutov

import SpeechKitAuth
import SpeechKitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

fun main() {
    val clientSecret = "ODkwNzBmOTYtZmI5MS00YjU5LTgzZWQtZDNkZTEyOTI1MWE2OjM2ZjA0NDczLTllMzAtNDFlOC05ZTFmLTg1YmQxNWFjMmFhMw=="
    val scope = "SALUTE_SPEECH_PERS"
    val audioFilePath = "outm.mp3"
    val authManager = SpeechKitAuth(clientSecret, scope)

    // Create a logger
    val logger = org.slf4j.LoggerFactory.getLogger("Main")

    try {
        SpeechKitClient(authManager, scope).use { client ->
            CoroutineScope(Dispatchers.Unconfined).launch {
                client.recognizeAsFlow(File(audioFilePath)).collect { result ->
                    when (result) {
                        is RecognitionResult.Transcription -> {
                            logger.info("Текст: ${result.text}")
                            if (result.isFinal) logger.info("ФИНАЛЬНЫЙ РЕЗУЛЬТАТ: ${result.text}")
                        }

                        is RecognitionResult.BackendInfo ->
                            logger.info("Бэкенд: модель=${result.modelName}, версия=${result.modelVersion}")

                        is RecognitionResult.Insight ->
                            logger.info("Insight: ${result.data}")

                        is RecognitionResult.VadInfo ->
                            logger.info("Голосовая активность: ${if (result.hasVoice) "Есть голос" else "Нет голоса"}")

                        is RecognitionResult.Error ->
                            logger.error("Ошибка: ${result.message}")
                    }
                }
            }
        }
    } catch (e: Exception) {
        if (e.message?.contains("UNAUTHENTICATED") == true) {
            println("Ошибка аутентификации, пробуем обновить токен и повторить запрос")
            try {
                authManager.refreshAccessToken()
                // Повторяем запрос с новым токеном
                SpeechKitClient(authManager, scope).use { client ->
                    CoroutineScope(Dispatchers.Unconfined).launch {
                        client.recognizeAsFlow(File(audioFilePath)).collect { result ->
                            when (result) {
                                is RecognitionResult.Transcription -> {
                                    logger.info("Текст: ${result.text}")
                                    if (result.isFinal) logger.info("ФИНАЛЬНЫЙ РЕЗУЛЬТАТ: ${result.text}")
                                }

                                is RecognitionResult.BackendInfo ->
                                    logger.info("Бэкенд: модель=${result.modelName}, версия=${result.modelVersion}")

                                is RecognitionResult.Insight ->
                                    logger.info("Insight: ${result.data}")

                                is RecognitionResult.VadInfo ->
                                    logger.info("Голосовая активность: ${if (result.hasVoice) "Есть голос" else "Нет голоса"}")

                                is RecognitionResult.Error ->
                                    logger.error("Ошибка: ${result.message}")
                            }
                        }
                    }
                }
            } catch (refreshError: Exception) {
                println("Не удалось обновить токен: ${refreshError.message}")
            }
        } else {
            println("Произошла ошибка: ${e.message}")
        }
    }
}
