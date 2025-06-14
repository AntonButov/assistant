package `resul-text`

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import kotlinx.coroutines.flow.Flow

@Composable
fun rememberResultTextState(flow: Flow<ResultState>): State<String> =
    produceState(initialValue = "") {
        flow.collect { newState ->
            when (newState) {
                is ResultStateText -> value += newState.text
                is ResultClean -> value = ""
            }
        }
    }