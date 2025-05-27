import TODO.Salutespeech
import TODO.SmartSpeechGrpc
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import com.google.protobuf.ByteString
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.flatMapLatest
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
        val isFinal: Boolean,
    ) : RecognitionResult()

    /**
     * Информация о модели распознавания речи
     */
    data class BackendInfo(
        val modelName: String,
        val modelVersion: String,
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
    private val channel = NettyChannelBuilder.forTarget("smartspeech.sber.ru")
        .useTransportSecurity()
        .sslContext(
            GrpcSslContexts.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build()
        )
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

    fun recognizeMicrophone() =
        microPhoneFlow(logger).bytesToArray()

    @OptIn(ExperimentalCoroutinesApi::class)
    fun recognizeFile(
        audioFile: File,
        languageCode: String = "ru-RU",
        sampleRate: Int = 16000,
    ): Flow<RecognitionResult> =
        fileFlow(audioFile)
            .bytesToArray(languageCode, sampleRate)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun Flow<ByteArray>.bytesToArray(
        languageCode: String = "ru-RU",
        sampleRate: Int = 16000
    ): Flow<RecognitionResult> = flatMapLatest { audioBytes ->

        logger.info("получено байт ${audioBytes.size}")

        callbackFlow {
            // Настраиваем опции распознавания
            val options =
                Salutespeech.RecognitionOptions.newBuilder()
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
            val optionsRequest =
                Salutespeech.RecognitionRequest.newBuilder()
                    .setOptions(options)
                    .build()

            // Создаем запрос с аудиоданными
            val audioChunkRequest =
                Salutespeech.RecognitionRequest.newBuilder()
                    .setAudioChunk(ByteString.copyFrom(audioBytes))
                    .build()

            logger.info("Отправка запроса на распознавание...")

            // Создаем обработчик ответов
            val streamObserver =
                object : io.grpc.stub.StreamObserver<Salutespeech.RecognitionResponse> {
                    override fun onNext(response: Salutespeech.RecognitionResponse) {
                        when {
                            response.hasTranscription() -> {
                                val transcription = response.transcription
                                val isEou = transcription.eou

                                val resultText =
                                    transcription.resultsList.joinToString(" ") { result ->
                                        result.normalizedText.ifEmpty { result.text }
                                    }

                                if (resultText.isNotEmpty()) {
                                    trySend(RecognitionResult.Transcription(resultText, isEou))
                                    //logger.info("Распознано: $resultText (финальный: $isEou)")
                                }
                            }

                            response.hasBackendInfo() -> {
                                val backendInfo = response.backendInfo
                                trySend(
                                    RecognitionResult.BackendInfo(
                                        modelName = backendInfo.modelName,
                                        modelVersion = backendInfo.modelVersion,
                                    ),
                                )
                            }

                            response.hasInsight() -> {
                                trySend(RecognitionResult.Insight(response.insight.insightResult))
                                logger.info("Получен insight: ${response.insight.insightResult}")
                            }

                            response.hasVad() -> {
                                val vadInfo = response.vad
                                trySend(RecognitionResult.VadInfo(true)) // vadInfo.hasVoice
                                // logger.info("Получен VAD результат: ") // ${vadInfo.hasVoice}")
                            }
                        }
                    }

                    override fun onError(t: Throwable) {
                        logger.log(Level.SEVERE, "Ошибка при распознавании речи", t)
                        trySend(RecognitionResult.Error("Ошибка при распознавании: ${t.message}", t))
                        close(t)
                    }

                    override fun onCompleted() {
                        // logger.info("Распознавание завершено успешно")
                        close()
                    }
                }

            // Получаем requestObserver для отправки запросов
            val requestObserver = stub.recognize(streamObserver)

            // logger.info("Отправка настроек распознавания...")
            requestObserver.onNext(optionsRequest)

            // logger.info("Отправка аудиоданных (${audioBytes.size} байт)...")
            requestObserver.onNext(audioChunkRequest)

            // logger.info("Сигнализация о завершении запроса...")
            requestObserver.onCompleted()

            awaitClose {
                //  logger.info("Закрытие клиента распознавания речи")
            }
        }
    }

    override fun close() {
        logger.info("Закрытие клиента распознавания речи")
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}
