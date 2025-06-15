import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import kotlinx.coroutines.flow.Flow
import recognizer.RecognizerInterface
import recognizer.StateButton
import `resul-text`.ResultState

interface MainViewModel {
    val stateButton: State<StateButton>
    val outputFlow: Flow<ResultState>
    fun click()

    fun close()
}

class MainViewModelImpl(private val recognizer: RecognizerInterface): MainViewModel {
    private val _stateButton: MutableState<StateButton> = mutableStateOf(StateButton.Idle)
    override val stateButton: State<StateButton> = _stateButton
    override val outputFlow: Flow<ResultState>
        get() = recognizer.outputFlow

    override fun click() {
        when (_stateButton.value) {
            StateButton.Idle -> {
                recognizer.start()
                _stateButton.value = StateButton.Start
            }

            StateButton.Start -> {
                recognizer.close()
                _stateButton.value = StateButton.Idle
            }
        }
    }

    override fun close() {
        recognizer.close()
    }
}
