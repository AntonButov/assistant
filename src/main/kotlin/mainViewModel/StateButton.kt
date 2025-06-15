package mainViewModel

sealed interface StateButton {
    data object Idle : StateButton

    data object Start : StateButton
}
