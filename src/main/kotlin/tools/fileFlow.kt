package tools

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File

fun fileFlow(audioFile: File): Flow<ByteArray> =
    flow {
        if (!audioFile.exists()) {
            throw IllegalArgumentException("Файл не существует: ${audioFile.absolutePath}")
        }

        val audioBytes = audioFile.readBytes()
        emit(audioBytes)
    }
