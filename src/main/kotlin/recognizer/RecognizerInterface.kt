package recognizer

import androidx.compose.runtime.State
import kotlinx.coroutines.flow.Flow
import `resul-text`.ResultState

interface RecognizerInterface {
    val outputFlow: Flow<ResultState>
    val stateButton: State<StateButton>

    fun click()
}
