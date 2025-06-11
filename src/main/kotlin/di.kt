
import dagger.dsl.core.DaggerDsl
import dagger.dsl.core.component
import dagger.dsl.core.get

interface Component {
    fun getRecogniser(): RecognizerInterface
}

@DaggerDsl
fun di() = component<Component> {
        bind<RecognizerInterface, RecognizerNew>()
    }
