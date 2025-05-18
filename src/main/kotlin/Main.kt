package tech.antonbutov

import com.google.gson.Gson
import com.google.gson.JsonObject
import okhttp3.*
import okio.ByteString
import okio.ByteString.Companion.toByteString
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import java.io.ByteArrayOutputStream
import java.util.Base64
import java.util.UUID

fun main() {
    // API ключ Яндекс SpeechKit (получите в консоли Яндекс.Облака)
    val apiKey = "YOUR_YANDEX_API_KEY"

    // Настройка аудио
    val sampleRate = 16000f
    val sampleSizeInBits = 16
    val channels = 1
    val signed = true
    val bigEndian = false

    val format = AudioFormat(sampleRate, sampleSizeInBits, channels, signed, bigEndian)
    val info = DataLine.Info(TargetDataLine::class.java, format)

    if (!AudioSystem.isLineSupported(info)) {
        println("Line not supported")
        return
    }

    println("Инициализация распознавания речи через WebSocket...")

    // Создаем клиент OkHttp
    val client = OkHttpClient.Builder()
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    // Создаем уникальный идентификатор сессии
    val sessionId = UUID.randomUUID().toString()

    // Создаем WebSocket запрос
    val request = Request.Builder()
        .url("wss://stt.api.cloud.yandex.net/speech/v1/stt:websocket")
        .addHeader("Authorization", "Api-Key $apiKey")
        .build()

    // Для ожидания завершения распознавания
    val completionLatch = CountDownLatch(1)

    // Накопленные результаты распознавания
    val recognitionResults = StringBuilder()

    // Обработчик WebSocket событий
    val webSocketListener = object : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            println("WebSocket открыт, начинаем отправку настроек...")

            // Отправляем настройки распознавания
            val configMessage = """
                {
                    "config": {
                        "specification": {
                            "languageCode": "ru-RU",
                            "profanityFilter": false,
                            "model": "general",
                            "format": "lpcm",
                            "sampleRateHertz": 16000
                        }
                    }
                }
            """.trimIndent()

            webSocket.send(configMessage)

            // Начинаем запись и отправку аудио
            startRecording(webSocket, format)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            try {
                val response = Gson().fromJson(text, JsonObject::class.java)
                if (response.has("results")) {
                    val results = response.getAsJsonArray("results")
                    if (results.size() > 0) {
                        val result = results.get(0).asJsonObject
                        if (result.has("alternatives") &&
                            result.getAsJsonArray("alternatives").size() > 0) {

                            val alternative = result.getAsJsonArray("alternatives").get(0).asJsonObject
                            val text = alternative.get("text").asString
                            val isFinal = result.get("final").asBoolean

                            if (isFinal) {
                                recognitionResults.append(text).append(" ")
                                println("Распознано: $text")
                            }
                        }
                    }
                }

                if (response.has("endOfUtterance") && response.get("endOfUtterance").asBoolean) {
                    println("Распознавание завершено")
                    // Мы не закрываем соединение здесь, т.к. может быть еще не все результаты получены
                }
            } catch (e: Exception) {
                println("Ошибка при обработке сообщения: ${e.message}")
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            println("WebSocket закрыт: $reason")
            completionLatch.countDown()
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            println("Ошибка WebSocket: ${t.message}")
            t.printStackTrace()
            completionLatch.countDown()
        }
    }

    // Открываем WebSocket соединение
    val webSocket = client.newWebSocket(request, webSocketListener)

    // Ждем завершения распознавания (макс. 30 секунд)
    completionLatch.await(30, TimeUnit.SECONDS)

    // Выводим итоговый результат
    println("\nФинальный результат распознавания:")
    println(recognitionResults.toString())

    // Освобождаем ресурсы
    client.dispatcher.executorService.shutdown()
}

// Функция для записи и отправки аудио через WebSocket
fun startRecording(webSocket: WebSocket, format: AudioFormat) {
    println("Запись... (говорите сейчас)")

    try {
        val line = AudioSystem.getLine(DataLine.Info(TargetDataLine::class.java, format)) as TargetDataLine
        line.open(format)
        line.start()

        val buffer = ByteArray(3200) // 100мс при 16кГц 16бит моно
        val recordTimeSeconds = 5 // Запись 5 секунд (можно изменить)
        val totalBytes = (format.sampleRate * format.sampleSizeInBits / 8 * recordTimeSeconds).toInt()
        var bytesRead = 0

        // Отдельный поток для записи и отправки аудио
        Thread {
            try {
                while (bytesRead < totalBytes) {
                    val numBytesRead = line.read(buffer, 0, buffer.size)
                    if (numBytesRead > 0) {
                        // Отправляем аудиоданные через WebSocket
                        webSocket.send(buffer.toByteString(0, numBytesRead))
                        bytesRead += numBytesRead
                    }
                }

                println("Запись завершена.")
                line.stop()
                line.close()

                // Отправляем сигнал завершения аудио
                webSocket.send(okio.ByteString.EMPTY)

                // Закрываем соединение через 2 секунды (чтобы успели прийти все результаты)
                Thread.sleep(2000)
                webSocket.close(1000, "Распознавание завершено")
            } catch (e: Exception) {
                println("Ошибка при записи/отправке аудио: ${e.message}")
                webSocket.close(1001, "Ошибка при записи аудио")
            }
        }.start()

    } catch (e: Exception) {
        println("Ошибка при запуске записи: ${e.message}")
        webSocket.close(1001, "Не удалось инициализировать аудиоустройство")
    }
}
