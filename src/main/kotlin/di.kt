
import com.aallam.openai.client.OpenAI
import org.koin.dsl.module

val appModule =
    module {
        factory { SpeechKitAuth() }
        factory { MicrophoneSharedFlow() }
        factory<StringBag> { StringBagImpl() }
        factory { RecognizerNew(get(), get(), get()) }
        single { OpenAI(PropertyLoader.getProperty("openai.key")) }
    }
