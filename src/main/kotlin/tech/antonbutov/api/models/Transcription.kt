package tech.antonbutov.api.models

sealed class RecognitionResult {
    data class BackendInfo(
        val modelName: String,
        val modelVersion: String,
    ) : RecognitionResult()

    data class Transcription(
        val text: String,
        val isFinal: Boolean,
    ) : RecognitionResult()

    data class Insight(val data: String) : RecognitionResult()

    data class VadInfo(val hasVoice: Boolean) : RecognitionResult()

    data class Error(val message: String, val cause: Throwable? = null) : RecognitionResult()
}
