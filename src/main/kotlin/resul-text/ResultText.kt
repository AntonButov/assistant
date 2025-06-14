package `resul-text`

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import kotlinx.coroutines.flow.flowOf

@Composable
fun ResultText(resultTextState: State<String>) {
    Text(resultTextState.value)
}

@Preview
@Composable
fun ResultTextPreview() {
    val resultTextState = rememberResultTextState(flowOf("Hello World"))
    ResultText(resultTextState)
}
