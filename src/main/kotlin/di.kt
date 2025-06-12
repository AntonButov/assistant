
import com.aallam.openai.client.OpenAI
import org.koin.dsl.module

val appModule =
    module {
        factory { SpeechKitAuth() }
        factory { MicrophoneSharedFlow() }
        single { StringBag() }
        factory { RecognizerNew(get(), get(), get()) }
        single { OpenAI(PropertyLoader.getProperty("openai.key")) }
    }
