package recognizer

import androidx.compose.runtime.State
import kotlinx.coroutines.flow.Flow

interface RecognizerInterface {
    val outputFlow: Flow<String>
    val stateButton: State<StateButton>

    fun click()
}
