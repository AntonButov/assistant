
import com.aallam.openai.client.OpenAI
import microphoneSharedFlow.MicrophoneSharedFlow
import openAi.ChatCompletionMapperImpl
import openAi.ChatCompletionRequestMapperImpl
import openAi.OpenAiImpl
import org.koin.dsl.module
import recognizer.RecognizerNew
import tools.PropertyLoader

val appModule =
    module {
        factory { SpeechKitAuth() }
        factory { MicrophoneSharedFlow() }
        single<StringBag> { StringBagImpl(get()) }
        factory { RecognizerNew(get(), get(), get()) }
        single { OpenAI(PropertyLoader.getProperty("openai.key")) }
        factory { ChatCompletionRequestMapperImpl() }
        factory { ChatCompletionMapperImpl() }
        factory { OpenAiImpl(get(), get(), get()) }
    }
