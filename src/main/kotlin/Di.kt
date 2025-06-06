
import dagger.dsl.core.DaggerDsl
import dagger.dsl.core.component

interface Component {
    fun getRecogniser(): RecognizerInterface
}

@DaggerDsl
val di = component<Component> {
        bind<RecognizerInterface, RecognizerNew>()
    }
