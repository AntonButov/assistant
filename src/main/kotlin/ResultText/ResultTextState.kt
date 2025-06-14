package ResultText

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

@Composable
fun rememberResultTextState(flow: Flow<String>): ResultTextState {
    val scope = rememberCoroutineScope()
    return remember {
        ResultTextState(flow, scope)
    }
}

class ResultTextState(flow: Flow<String>, scope: CoroutineScope) {
    val state = mutableStateOf("")

    init {
        flow.onEach {
            state.value += it
        }.launchIn(scope)
    }
}
