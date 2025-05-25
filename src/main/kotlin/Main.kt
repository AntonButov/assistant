package tech.antonbutov

import SpeechKitClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

fun main() {
    val authKey = ""
    val scope = "SALUTE_SPEECH_PERS"
    val audioFilePath = "outm.mp3"

    // Create a logger
    val logger = org.slf4j.LoggerFactory.getLogger("Main")

    SpeechKitClient(
        accessKey = authKey,
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

                    is RecognitionResult.Error ->
                        logger.error("Ошибка: ${result.message}")
                }
            }
        }
    }
}
