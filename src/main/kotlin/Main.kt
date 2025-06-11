import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.*
import org.koin.core.context.GlobalContext.startKoin

fun main() =
    application {
        val koin = startKoin {
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

fun applyResult(result: RecognitionResult) {
    val logger = LoggerAssistant
    logger.info("Результат распознавания: $result")
    when (result) {
        is RecognitionResult.Transcription -> {
            logger.info("Текст: ${result.text}")
            if (result.isFinal) logger.info("ФИНАЛЬНЫЙ РЕЗУЛЬТАТ: ${result.text}")
        }

        is RecognitionResult.BackendInfo -> {
        }

        is RecognitionResult.Insight -> {
            logger.info("Insight: ${result.data}")
        }

        is RecognitionResult.VadInfo ->
            logger.info("Голосовая активность: ${if (result.hasVoice) "Есть голос" else "Нет голоса"}")

        is RecognitionResult.Error -> {
            logger.info("Ошибка: ${result.message}")
            if (result.message.contains("UNAUTHENTICATED")) {
                logger.info("Токен устарел, получаем новый...")
                // authManager.refreshAccessToken()
            }
        }
    }
}

@Composable
@Preview
fun App(recognizerNew: RecognizerNew) {
    val scope = rememberCoroutineScope()
    scope.launch {
        recognizerNew
            .recognizedFlow
            .collect { result ->
                applyResult(result)
            }
    }

    var text by remember { mutableStateOf("Start") }

    MaterialTheme {
        Button(
            onClick = {
                text = "Hello, Desktop!"
                // recognizerNew.run()
            },
            modifier = Modifier.testTag("button"),
        ) {
            Text("Start")
        }
    }
}
