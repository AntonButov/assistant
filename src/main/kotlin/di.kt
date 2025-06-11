
import org.koin.dsl.module

val appModule = module {
    factory { SpeechKitAuth() }
    factory { MicrophoneSharedFlow() }
    factory { RecognizerNew(get(), get()) }
}