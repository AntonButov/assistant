package resultText

import androidx.compose.runtime.Composable
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.Flow

@Composable
fun rememberResultTextState(flow: Flow<String>) = produceState(initialValue = "") {
    flow.collect { newText ->
        value += newText
    }
}