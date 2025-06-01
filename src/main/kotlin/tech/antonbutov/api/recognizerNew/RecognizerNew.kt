package tech.antonbutov.api.recognizerNew

import TODO.Salutespeech
import TODO.SmartSpeechGrpc
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import com.google.protobuf.ByteString
import io.grpc.netty.shaded.io.grpc.netty.GrpcSslContexts
import io.grpc.netty.shaded.io.grpc.netty.NettyChannelBuilder
import io.grpc.netty.shaded.io.netty.handler.ssl.util.InsecureTrustManagerFactory
import io.grpc.stub.StreamObserver
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import tech.antonbutov.api.models.RecognitionResult
import java.io.Closeable
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger

class RecognizerNew(
    private val accessKey: String,
    private val sourceFlow: Flow<ByteArray>,
    private val coroutineScope: CoroutineScope,
) : Closeable {

    private val _recognisedFlow: MutableStateFlow<RecognitionResult> = MutableStateFlow(RecognitionResult.Transcription("Strart", false))
    val recognizedFlow: Flow<RecognitionResult> = _recognisedFlow
    private val scope: String = "SALUTE_SPEECH_PERS"

    private val languageCode = "ru-RU"
    private val sampleRate = 16000

    private val logger = Logger.getLogger(RecognizerNew::class.java.name)
    private val channel = NettyChannelBuilder.forTarget("smartspeech.sber.ru")
        .useTransportSecurity()
        .sslContext(
            GrpcSslContexts.forClient()
                .trustManager(InsecureTrustManagerFactory.INSTANCE)
                .build()
        )
        .build()

    private val stub by lazy { createStub() }

    // Настраиваем опции распознавания
    private val optionsRequest = createOptions()

    private val streamObserver by lazy {
        createStreamObserver()
    }

    private val requestObserver by lazy {
        stub.recognize(streamObserver).also {
            it.onNext(optionsRequest)
        }
    }

    init {
        coroutineScope.launch {
            sourceFlow
                .onEach {
                    requestObserver.onNext(it.toChunk())
                }
                .onCompletion {
                    requestObserver.onCompleted() // TODO нужно подумать перезапускать
                }
                .collect()
        }
    }

    override fun close() {
        logger.info("Закрытие клиента распознавания речи")
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }

    private fun createStreamObserver() =
        object : StreamObserver<Salutespeech.RecognitionResponse> {
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
                            _recognisedFlow.update {
                                RecognitionResult.Transcription(resultText, isEou)
                            }
                            //logger.info("Распознано: $resultText (финальный: $isEou)")
                        }
                    }

                    response.hasBackendInfo() -> {
                        val backendInfo = response.backendInfo
                        _recognisedFlow.update {
                            RecognitionResult.BackendInfo(
                                modelName = backendInfo.modelName,
                                modelVersion = backendInfo.modelVersion,
                            )
                        }
                    }

                    response.hasInsight() -> {
                        _recognisedFlow.update {
                            RecognitionResult.Insight(response.insight.insightResult)
                        }
                        logger.info("Получен insight: ${response.insight.insightResult}")
                    }

                    response.hasVad() -> {
                        _recognisedFlow.update {
                            RecognitionResult.VadInfo(true)
                        }
                    }
                }
            }

            override fun onError(t: Throwable) {
                logger.log(Level.SEVERE, "Ошибка при распознавании речи", t)
                _recognisedFlow.update {
                    RecognitionResult.Error("Ошибка при распознавании: ${t.message}", t)
                }
                // close(t)
            }

            override fun onCompleted() {
                logger.info("Распознавание завершено успешно")
                // close()
            }
        }

    private fun createOptions(): Salutespeech.RecognitionRequest {
        val options =
            Salutespeech.RecognitionOptions.newBuilder()
                .setAudioEncoding(Salutespeech.RecognitionOptions.AudioEncoding.PCM_S16LE)
                .setSampleRate(sampleRate)
                .setChannelsCount(1)
                .setLanguage(languageCode)
                // Включаем поддержку множественных высказываний
                .setEnableMultiUtterance(Salutespeech.OptionalBool.newBuilder().setEnable(true).build())
                // Настраиваем распознавание длинных высказываний
                .setEnableLongUtterances(Salutespeech.OptionalBool.newBuilder().setEnable(true).build())
                .build()

        // Создаем запрос с опциями
        return Salutespeech.RecognitionRequest.newBuilder()
                .setOptions(options)
                .build()
    }

    private fun createStub(): SmartSpeechGrpc.SmartSpeechStub {
            val headers = Metadata()
            val authKey = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
            val scopeKey = Metadata.Key.of("Content-Scope", Metadata.ASCII_STRING_MARSHALLER)
            headers.put(authKey, "Bearer $accessKey")
            headers.put(scopeKey, scope)

            // Создаем стаб с установленными заголовками
            return SmartSpeechGrpc.newStub(channel)
                .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
                .withDeadlineAfter(30, TimeUnit.SECONDS)
    }
}

private fun ByteArray.toChunk() = Salutespeech.RecognitionRequest.newBuilder()
            .setAudioChunk(ByteString.copyFrom(this))
            .build()
