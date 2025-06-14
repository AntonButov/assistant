package ResultText

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import io.ktor.websocket.Frame

@Composable
fun ResultText(resultTextState: ResultTextState = remember { ResultTextStateImpl() }) {
    Text(resultTextState.text)
}

@Preview
@Composable
fun ResultTextPreview() {
    val resultTextState = remember { ResultTextStateImpl() }
    resultTextState.addText("Hello")
    resultTextState.addText(" World")
    ResultText(resultTextState)
}