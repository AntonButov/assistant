import kotlinx.coroutines.*
import tech.antonbutov.api.models.RecognitionResult
import tech.antonbutov.api.recognizerNew.RecognizerNew

fun main() {

    val authManager = SpeechKitAuth()
    val accessToken = runBlocking {
        authManager.getAccessToken()
    }

    val scope = CoroutineScope(Dispatchers.IO)

    val microphoneSgaredFlow = MicrophoneSharedFlow()

    val recognizerNew = RecognizerNew(
        accessKey = accessToken,
        sourceFlow = microphoneSgaredFlow.audioFlow,
        coroutineScope = scope
    )

    scope.launch {
        recognizerNew
            .recognizedFlow
            .collect { result ->
                applyResult(result, authManager)
            }
    }

    microphoneSgaredFlow.run()

    runBlocking {
        delay(2000)
    }

    recognizerNew.close()
}

    suspend fun applyResult(result: RecognitionResult, authManager: SpeechKitAuth, ) {
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
