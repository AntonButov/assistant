import androidx.compose.runtime.State

interface RecognizerInterface {
    val stateButton: State<StateButton>

    fun click()
}
