import TODO.Salutespeech
import TODO.SmartSpeechGrpc
import com.google.protobuf.ByteString
import io.grpc.ManagedChannel
import io.grpc.ManagedChannelBuilder
import io.grpc.Metadata
import io.grpc.stub.MetadataUtils
import java.io.Closeable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.logging.Level
import java.util.logging.Logger
import javax.sound.sampled.*
import kotlin.concurrent.thread

class SpeechRecorderRecognizer(
    private val authorizationKey: String,
    private val scope: String = "SALUTE_SPEECH_PERS"
) : Closeable {
    private val logger = Logger.getLogger(SpeechRecorderRecognizer::class.java.name)
    private val channel: ManagedChannel
    private val stub: SmartSpeechGrpc.SmartSpeechStub
    private var isRecording = false
    private val audioFormat = AudioFormat(16000f, 16, 1, true, false)

    init {
        // Создаем канал для подключения к серверу SmartSpeech
        channel = ManagedChannelBuilder
            .forAddress("smartspeech.sber.ru", 443)
            .useTransportSecurity()
            .build()

        // Создаем заголовки для авторизации
        val headers = Metadata()
        val authKey = Metadata.Key.of("Authorization", Metadata.ASCII_STRING_MARSHALLER)
        val scopeKey = Metadata.Key.of("Content-Scope", Metadata.ASCII_STRING_MARSHALLER)
        headers.put(authKey, "Bearer $authorizationKey")
        headers.put(scopeKey, scope)

        // Создаем стаб с установленными заголовками
        stub = SmartSpeechGrpc.newStub(channel)
            .withInterceptors(MetadataUtils.newAttachHeadersInterceptor(headers))
            .withDeadlineAfter(120, TimeUnit.SECONDS)
    }

    /**
     * Запись звука с микрофона и потоковое распознавание
     */
    fun recordAndRecognize(
        recordDurationSeconds: Int = 0, // 0 = запись до нажатия Enter
        languageCode: String = "ru-RU"
    ) {
        // Создаем объект для захвата аудио
        val targetInfo = DataLine.Info(TargetDataLine::class.java, audioFormat)
        if (!AudioSystem.isLineSupported(targetInfo)) {
            logger.severe("Микрофон с указанным форматом не поддерживается")
            return
        }

        val microphone = AudioSystem.getLine(targetInfo) as TargetDataLine
        microphone.open(audioFormat)
        microphone.start()

        logger.info("Начало записи с микрофона")
        println("=== Запись началась. Говорите в микрофон. ===")
        println("Нажмите Enter для завершения записи${if (recordDurationSeconds > 0) " или дождитесь $recordDurationSeconds секунд" else ""}.")

        // Настраиваем опции распознавания
        val options = Salutespeech.RecognitionOptions.newBuilder()
            .setAudioEncoding(Salutespeech.RecognitionOptions.AudioEncoding.PCM_S16LE)
            .setSampleRate(16000)
            .setChannelsCount(1)
            .setLanguage(languageCode)
            .setEnablePartialResults(Salutespeech.OptionalBool.newBuilder().setEnable(true).build()) // Получаем промежуточные результаты
            .setEnableMultiUtterance(Salutespeech.OptionalBool.newBuilder().setEnable(true).build()) // Распознаем несколько высказываний
            .build()

        // Создаем запрос с опциями
        val optionsRequest = Salutespeech.RecognitionRequest.newBuilder()
            .setOptions(options)
            .build()

        // Создаем синхронизатор для ожидания завершения потока
        val finishLatch = CountDownLatch(1)
        val resultBuilder = StringBuilder()

        // Создаем обработчик ответов
        val streamObserver = object : io.grpc.stub.StreamObserver<Salutespeech.RecognitionResponse> {
            override fun onNext(response: Salutespeech.RecognitionResponse) {
                when {
                    response.hasTranscription() -> {
                        val transcription = response.transcription
                        val isEou = transcription.eou

                        for (result in transcription.resultsList) {
                            val text = if (result.normalizedText.isNotEmpty()) {
                                result.normalizedText
                            } else {
                                result.text
                            }

                            // Если это промежуточный результат, печатаем с возвратом каретки
                            if (!isEou) {
                                print("\r${" ".repeat(100)}\r>> $text")
                            } else {
                                // Если финальный результат, добавляем в итоговый текст
                                resultBuilder.append(text).append(" ")
                                println("\r${" ".repeat(100)}\r✓ $text")
                            }
                        }
                    }
                    response.hasBackendInfo() -> {
                        val backendInfo = response.backendInfo
                        logger.info("Получена информация о бэкенде: модель=${backendInfo.modelName}, версия=${backendInfo.modelVersion}")
                    }
                }
            }

            override fun onError(t: Throwable) {
                logger.log(Level.SEVERE, "Ошибка при распознавании речи", t)
                finishLatch.countDown()
            }

            override fun onCompleted() {
                println("\n=== Распознавание завершено ===")
                println(resultBuilder.toString().trim())
                finishLatch.countDown()
            }
        }

        // Получаем requestObserver для отправки запросов
        val requestObserver = stub.recognize(streamObserver)

        try {
            // Отправляем сначала настройки
            requestObserver.onNext(optionsRequest)

            // Запускаем отдельный поток для прослушивания ввода с клавиатуры
            val inputThread = thread {
                System.`in`.read()
                isRecording = false
                println("\nЗавершение записи...")
            }

            // Буфер для чтения аудиоданных
            val buffer = ByteArray(3200) // 100 мс аудио при 16кГц 16-бит
            isRecording = true

            // Запускаем таймер для автоматического завершения, если указана длительность
            val endTime = if (recordDurationSeconds > 0)
                System.currentTimeMillis() + recordDurationSeconds * 1000L
            else
                Long.MAX_VALUE

            // Читаем и отправляем аудиоданные
            while (isRecording && System.currentTimeMillis() < endTime) {
                val bytesRead = microphone.read(buffer, 0, buffer.size)
                if (bytesRead > 0) {
                    // Создаем запрос с аудиоданными
                    val audioChunkRequest = Salutespeech.RecognitionRequest.newBuilder()
                        .setAudioChunk(ByteString.copyFrom(buffer, 0, bytesRead))
                        .build()

                    // Отправляем аудиоданные
                    requestObserver.onNext(audioChunkRequest)
                }
            }

            // Останавливаем запись
            microphone.stop()
            microphone.close()

            // Прерываем поток ввода, если запись закончилась по таймеру
            if (System.currentTimeMillis() >= endTime) {
                inputThread.interrupt()
            }

            // Сигнализируем о завершении запросов
            requestObserver.onCompleted()

            // Ждем завершения распознавания
            finishLatch.await(60, TimeUnit.SECONDS)

        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Ошибка при обработке аудио", e)
            requestObserver.onError(e)
            throw e
        }
    }

    override fun close() {
        logger.info("Закрытие клиента распознавания речи")
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS)
    }
}