package ResultText

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.flow.flowOf

@Composable
fun ResultText(resultTextState: ResultTextState) {
    Text(resultTextState.state.value)
}

@Preview
@Composable
fun ResultTextPreview() {
    val resultTextState = rememberResultTextState(flowOf("Hello World"))
    ResultText(resultTextState)
}
