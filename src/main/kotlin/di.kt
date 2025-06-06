
import dagger.dsl.core.DaggerDsl
import dagger.dsl.core.component
import dagger.dsl.core.get

interface Component {
    fun getRecogniser(): MicrophoneSharedFlow
}

@DaggerDsl
fun di() = component<Component> {
        provides<MicrophoneSharedFlow> { MicrophoneSharedFlow() }
    }
