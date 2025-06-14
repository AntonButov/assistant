package `resul-text`

sealed interface ResultState
data class ResultStateText(val text: String): ResultState
data object ResultClean: ResultState