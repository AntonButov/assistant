
import org.koin.dsl.module

val appModule = module {
    factory { SpeechKitAuth() }
    factory { MicrophoneSharedFlow() }
    single { StringBuffer() }
    factory { RecognizerNew(get(), get(), get()) }
}