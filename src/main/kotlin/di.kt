
import com.aallam.openai.client.OpenAI
import com.aallam.openai.client.OpenAIConfig
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import mainViewModel.MainViewModel
import mainViewModel.MainViewModelImpl
import microphoneSharedFlow.SoundSharedFlow
import openAi.ChatCompletionMapper
import openAi.ChatCompletionMapperImpl
import openAi.ChatCompletionRequestMapper
import openAi.ChatCompletionRequestMapperImpl
import openAi.OpenAi
import openAi.OpenAiImpl
import org.koin.dsl.module
import recognizer.RecognizerInterface
import recognizer.RecognizerNew
/**
 * Utility for loading properties from configuration files.
 */
import tools.PropertyLoader

val appModule =
    module {
        factory { SpeechKitAuth() }
        factory { SoundSharedFlow() }
        single { createOpenAi() }
        single<StringBag> { StringBagImpl(get()) }
        factory<RecognizerInterface> { RecognizerNew(get(), get(), get(), get()) }
        factory<ChatCompletionRequestMapper> { ChatCompletionRequestMapperImpl() }
        factory<ChatCompletionMapper> { ChatCompletionMapperImpl() }
        single<OpenAi> { OpenAiImpl(get(), get(), get()) }
        single<MainViewModel> { MainViewModelImpl(get()) }
    }

private fun createOpenAi(): OpenAI {
    val openAKey = PropertyLoader.getProperty("openai.key")

    val config =
        OpenAIConfig(
            token = openAKey,
            httpClientConfig = {
                install(Logging) {
                    level = LogLevel.NONE
                }
            },
        )

    return OpenAI(config)
}
