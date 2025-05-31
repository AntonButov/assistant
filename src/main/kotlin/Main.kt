import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import tech.antonbutov.api.models.RecognitionResult

fun main() {

    // Создаем Basic Auth ключ
    val authorizationKey =
        "ODkwNzBmOTYtZmI5MS00YjU5LTgzZWQtZDNkZTEyOTI1MWE2OmYyZThlOTU4LTQ5Y2QtNDczYi04Y2EyLTJiNmY4NmIzYTk4OA=="
    val scope = "SALUTE_SPEECH_PERS"

    val authManager = SpeechKitAuth(authorizationKey, scope)
    val accessToken = authManager.getAccessToken()

    val recognizer = Recognizer(accessToken)

    val microphoneSgaredFlow = MicrophoneSharedFlow()

    CoroutineScope(Dispatchers.IO).launch {
        recognizer
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
