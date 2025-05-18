package tech.antonbutov

import com.google.cloud.speech.v1.RecognitionAudio
import com.google.cloud.speech.v1.RecognitionConfig
import com.google.cloud.speech.v1.RecognizeRequest
import com.google.cloud.speech.v1.SpeechClient
import com.google.cloud.speech.v1.RecognitionConfig.AudioEncoding
import com.google.protobuf.ByteString
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem
import javax.sound.sampled.DataLine
import javax.sound.sampled.TargetDataLine
import java.io.ByteArrayOutputStream

fun main() {
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

    val line = AudioSystem.getLine(info) as TargetDataLine
    line.open(format)
    line.start()

    println("Recording... (speak now)")

    // Запись аудио в память
    val out = ByteArrayOutputStream()
    val buffer = ByteArray(1024)
    val recordTime = 5 * sampleRate.toInt() // запись 5 секунд
    var bytesRead = 0

    while (bytesRead < recordTime) {
        val numBytesRead = line.read(buffer, 0, buffer.size)
        out.write(buffer, 0, numBytesRead)
        bytesRead += numBytesRead
    }

    println("Finished recording.")
    line.stop()
    line.close()

    val audioBytes = out.toByteArray()
    out.close()

    // Отправляем аудио в Google Speech-to-Text API
    try {
        SpeechClient.create().use { speechClient ->
            // Подготовка аудио для Google API
            val audio = RecognitionAudio.newBuilder()
                .setContent(ByteString.copyFrom(audioBytes))
                .build()

            // Настройка распознавания
            val config = RecognitionConfig.newBuilder()
                .setEncoding(AudioEncoding.LINEAR16)
                .setSampleRateHertz(sampleRate.toInt())
                .setLanguageCode("ru-RU") // Измените на нужный язык
                .build()

            val request = RecognizeRequest.newBuilder()
                .setConfig(config)
                .setAudio(audio)
                .build()

            // Отправка запроса и получение ответа
            val response = speechClient.recognize(request)

            println("Transcription results:")
            for (result in response.resultsList) {
                for (alternative in result.alternativesList) {
                    println("Transcription: ${alternative.transcript}")
                    println("Confidence: ${alternative.confidence}")
                }
            }
        }
    } catch (e: Exception) {
        println("Error occurred while sending audio to Google Speech-to-Text API: ${e.message}")
        e.printStackTrace()
    }
}
