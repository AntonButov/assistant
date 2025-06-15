import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import mainViewModel.MainViewModel
import mainViewModel.StateButton
import org.koin.core.context.GlobalContext.startKoin
import `resul-text`.ResultText
import `resul-text`.rememberResultTextState

fun main() =
    application {
        val koin =
            startKoin {
                modules(appModule)
            }
        val viewModel =
            remember {
                koin.koin.get<MainViewModel>()
            }

        Window(onCloseRequest = {
            viewModel.close()
            exitApplication()
        }) {
            App(viewModel)
        }
    }

@Composable
@Preview
fun App(viewModel: MainViewModel) {
    val resultTextState = rememberResultTextState(viewModel.outputFlow)
    MaterialTheme {
        Column {
            Button(
                onClick = {
                    viewModel.click()
                },
            ) {
                val buttonText =
                    when (viewModel.stateButton.value) {
                        StateButton.Idle -> "Start"
                        StateButton.Start -> "Stop"
                    }
                Text(buttonText)
            }
            ResultText(resultTextState)
        }
    }
}
