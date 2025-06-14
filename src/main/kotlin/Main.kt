import ResultText.ResultText
import ResultText.ResultTextStateImpl
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import org.koin.core.context.GlobalContext.startKoin
import recognizer.RecognizerNew
import recognizer.StateButton
import tools.LoggerAssistant

fun main() =
    application {
        val koin =
            startKoin {
                modules(appModule)
            }
        val recognizerNew =
            remember {
                koin.koin.get<RecognizerNew>()
            }
        Window(onCloseRequest = {
            recognizerNew.close()
            exitApplication()
        }) {
            App(recognizerNew)
        }
    }

@Composable
@Preview
fun App(recognizerNew: RecognizerNew) {
    val rememberResultTextState = remember { ResultTextStateImpl() }
    LoggerAssistant.info("collect")
    val outputFlow = remember { recognizerNew.outputFlow }
    val outputText = outputFlow.collectAsState(initial = "СтартТекст")
    LoggerAssistant.info("new test = ${outputText.value}")
    rememberResultTextState.addText(outputText.value)

    MaterialTheme {
        Column {
            Button(
                onClick = {
                    recognizerNew.click()
                },
            ) {
                val buttonText =
                    when (recognizerNew.stateButton.value) {
                        StateButton.Idle -> "Start"
                        StateButton.Start -> "Stop"
                    }
                Text(buttonText)
            }
            Text(
                text = "Текст",
            )
            Text(rememberResultTextState.text)
        }
    }
}
