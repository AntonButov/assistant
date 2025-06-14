package ResultText

interface ResultTextState {
    val text: String
    fun addText(text: String)
}

class ResultTextStateImpl() : ResultTextState {
    private val buffer = StringBuilder()
    override val text: String
        get() = buffer.toString()

    override fun addText(text: String) {
        buffer.append(text)
    }
}