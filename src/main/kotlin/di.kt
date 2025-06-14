
import com.aallam.openai.client.OpenAI
import microphoneSharedFlow.MicrophoneSharedFlow
import openAi.ChatCompletionMapper
import openAi.ChatCompletionMapperImpl
import openAi.ChatCompletionRequestMapper
import openAi.ChatCompletionRequestMapperImpl
import openAi.OpenAi
import openAi.OpenAiImpl
import org.koin.dsl.module
import recognizer.RecognizerNew
/**
 * Utility for loading properties from configuration files.
 */
import tools.PropertyLoader

val appModule =
    module {
        factory { SpeechKitAuth() }
        factory { MicrophoneSharedFlow() }
        single { OpenAI(PropertyLoader.getProperty("openai.key")) }
        single<StringBag> { StringBagImpl(get()) }
        factory { RecognizerNew(get(), get(), get(), get()) }
        factory<ChatCompletionRequestMapper> { ChatCompletionRequestMapperImpl() }
        factory<ChatCompletionMapper> { ChatCompletionMapperImpl() }
        single<OpenAi> { OpenAiImpl(get(), get(), get()) }
    }
