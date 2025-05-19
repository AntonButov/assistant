import TODO.Salutespeech
import TODO.SmartSpeechGrpc
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import com.google.protobuf.ByteString
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.io.Closeable
import java.io.File
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

/**
 * Sealed класс для различных типов результатов распознавания речи
 */
sealed class RecognitionResult {
    /**
     * Промежуточный или финальный результат распознавания текста
     */
    data class Transcription(
        val text: String,
        val isFinal: Boolean
    ) : RecognitionResult()

    /**
     * Информация о модели распознавания речи
     */
    data class BackendInfo(
        val modelName: String,
        val modelVersion: String
    ) : RecognitionResult()

    /**
     * Дополнительные метаданные о распознавании
     */
    data class Insight(val data: String) : RecognitionResult()

    /**
     * Информация о голосовой активности
     */
    data class VadInfo(val hasVoice: Boolean) : RecognitionResult()

    /**
     * Сообщение об ошибке
     */
    data class Error(val message: String, val cause: Throwable? = null) : RecognitionResult()
}

class SpeechKitClient(
    accessKey: String,
    scope: String = "SALUTE_SPEECH_PERS",
) : Closeable {

    private val logger = Logger.getLogger(SpeechKitClient::class.java.name)
    private val channel: ManagedChannel = ManagedChannelBuilder
        .forAddress("smartspeech.sber.ru", 443)
        .useTransportSecurity()
        .build()
    private val stub: SmartSpeechGrpc.SmartSpeechStub

    init {
        // Создаем заголовки для авторизации
        val headers = Metadata()
        val authKey = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
        val scopeKey = Metadata.Key.of("Content-Scope", Metadata.ASCII_STRING_MARSHALLER)
        headers.put(authKey, "Bearer $accessKey")
        headers.put(scopeKey, scope)

        // Создаем стаб с установленными заголовками
        stub = SmartSpeechGrpc.newStub(channel)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
            .withDeadlineAfter(30, TimeUnit.SECONDS)
    }

    /**
     * Отправляет аудиофайл на распознавание и возвращает Flow с результатами
     */
    fun recognizeAsFlow(
        audioFile: File,
        languageCode: String = "ru-RU",
        sampleRate: Int = 16000
    ): Flow<RecognitionResult> = callbackFlow {
        logger.info("Чтение аудиофайла: ${audioFile.absolutePath}")

        if (!audioFile.exists()) {
            close(IllegalArgumentException("Файл не существует: ${audioFile.absolutePath}"))
            return@callbackFlow
        }

        val audioBytes = audioFile.readBytes()
        logger.info("Прочитано ${audioBytes.size} байт")

        // Настраиваем опции распознавания
        val options = Salutespeech.RecognitionOptions.newBuilder()
            .setAudioEncoding(Salutespeech.RecognitionOptions.AudioEncoding.MP3)
            .setSampleRate(sampleRate)
            .setChannelsCount(1)
            .setLanguage(languageCode)
            // Включаем поддержку множественных высказываний
            .setEnableMultiUtterance(Salutespeech.OptionalBool.newBuilder().setEnable(true).build())
            // Настраиваем распознавание длинных высказываний
            .setEnableLongUtterances(Salutespeech.OptionalBool.newBuilder().setEnable(true).build())
            .build()

        // Создаем запрос с опциями
        val optionsRequest = Salutespeech.RecognitionRequest.newBuilder()
            .setOptions(options)
            .build()

        // Создаем запрос с аудиоданными
        val audioChunkRequest = Salutespeech.RecognitionRequest.newBuilder()
            .setAudioChunk(ByteString.copyFrom(audioBytes))
            .build()

        logger.info("Отправка запроса на распознавание...")

        // Создаем обработчик ответов
        val streamObserver = object : io.grpc.stub.StreamObserver<Salutespeech.RecognitionResponse> {
            override fun onNext(response: Salutespeech.RecognitionResponse) {
                when {
                    response.hasTranscription() -> {
                        val transcription = response.transcription
                        val isEou = transcription.eou

                        val resultText = transcription.resultsList.joinToString(" ") { result ->
                            if (result.normalizedText.isNotEmpty()) result.normalizedText else result.text
                        }

                        if (resultText.isNotEmpty()) {
                            trySend(RecognitionResult.Transcription(resultText, isEou))
                            logger.info("Распознано: $resultText (финальный: $isEou)")
                        }
                    }
                    response.hasBackendInfo() -> {
                        val backendInfo = response.backendInfo
                        trySend(RecognitionResult.BackendInfo(
                            modelName = backendInfo.modelName,
                            modelVersion = backendInfo.modelVersion
                        ))
                        logger.info("Получена информация о бэкенде: модель=${backendInfo.modelName}, версия=${backendInfo.modelVersion}")
                    }
                    response.hasInsight() -> {
                        trySend(RecognitionResult.Insight(response.insight.insightResult))
                        logger.info("Получен insight: ${response.insight.insightResult}")
                    }
                    response.hasVad() -> {
                        val vadInfo = response.vad
                        trySend(RecognitionResult.VadInfo(true)) // vadInfo.hasVoice
                        logger.info("Получен VAD результат: ")//${vadInfo.hasVoice}")
                    }
                }
            }

            override fun onError(t: Throwable) {
                logger.log(Level.SEVERE, "Ошибка при распознавании речи", t)
                trySend(RecognitionResult.Error("Ошибка при распознавании: ${t.message}", t))
                close(t)
            }

            override fun onCompleted() {
                logger.info("Распознавание завершено успешно")
                close()
            }
        }

        // Получаем requestObserver для отправки запросов
        val requestObserver = stub.recognize(streamObserver)

            //   try {
            // Отправляем сначала настройки
            logger.info("Отправка настроек распознавания...")
            requestObserver.onNext(optionsRequest)

            logger.info("Отправка аудиоданных (${audioBytes.size} байт)...")
            requestObserver.onNext(audioChunkRequest)

            logger.info("Сигнализация о завершении запроса...")
            requestObserver.onCompleted()
    //    } catch (e: Exception) {
        //    logger.info("Ошибка при отправке запроса")
        //    trySend(RecognitionResult.Error("Ошибка при отправке запроса: ${e.message}", e))
       //     requestObserver.onError(e)
      //      close(e)
//        }

        // Закрываем Flow когда канал закрывается
        awaitClose {
            logger.info("Flow закрыт")
        }
    }.catch { e ->
        // Перехватываем и отправляем любые ошибки как RecognitionResult.Error
        emit(RecognitionResult.Error("Необработанная ошибка: ${e.message}", e))
        logger.log(Level.SEVERE, "Необработанная ошибка в Flow", e)
        logger.info("Flow закрыт из-за ошибки")
    }

    /**
     * Синхронно отправляет аудиофайл на распознавание и возвращает текст
     * (для обратной совместимости)
     */
    suspend fun recognizeFile(
        audioFile: File,
        languageCode: String = "ru-RU",
        sampleRate: Int = 16000
    ): String = withContext(Dispatchers.IO) {
        var finalText = ""

        recognizeAsFlow(audioFile, languageCode, sampleRate).collect { result ->
            if (result is RecognitionResult.Transcription && result.isFinal) {
                finalText = result.text
            } else if (result is RecognitionResult.Error) {
                throw result.cause ?: RuntimeException(result.message)
            }
        }

        return@withContext finalText.ifEmpty { "Не удалось распознать речь" }
    }

    override fun close() {
        logger.info("Закрытие клиента распознавания речи")
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}