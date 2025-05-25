package tech.antonbutov

import SpeechKitAuth
import SpeechKitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.util.Base64

fun main() {
    // Ваши учетные данные Сбера
    val clientId = "ваш_client_id"
    val clientSecret = "ваш_client_secret"

    // Создаем Basic Auth ключ (Base64 от "clientId:clientSecret")
    val authorizationKey = Base64.getEncoder().encodeToString("$clientId:$clientSecret".toByteArray())

    val scope = "SALUTE_SPEECH_PERS"
    val audioFilePath = "outm.mp3"

    // Create a logger
    val logger = org.slf4j.LoggerFactory.getLogger("Main")

    try {
        // Создаем менеджер аутентификации с использованием Ktor
        val authManager = SpeechKitAuth(authorizationKey, scope)

        // Получаем токен доступа
        val accessToken = authManager.getAccessToken()
        logger.info("Получен токен доступа: $accessToken")

        // Используем токен для распознавания речи
        SpeechKitClient(
            accessKey = accessToken,
            scope = scope,
        ).use { client ->
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
                        is RecognitionResult.Error -> {
                            logger.error("Ошибка: ${result.message}")
                            // Если получаем ошибку аутентификации, обновляем токен и пробуем снова
                            if (result.message.contains("UNAUTHENTICATED")) {
                                logger.info("Токен устарел, получаем новый...")
                                authManager.refreshAccessToken()
                            }
                        }
                    }
                }
            }
        }
    } catch (e: Exception) {
        logger.error("Произошла ошибка: ${e.message}", e)
    }
}
