package recognizer

import androidx.compose.runtime.State
import kotlinx.coroutines.flow.Flow
import `resul-text`.ResultState

interface RecognizerInterface {
    val outputFlow: Flow<ResultState>

    fun start()

    fun close()
}
