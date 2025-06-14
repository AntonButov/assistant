import openAi.OpenAi

interface StringBag {
    fun add(string: String)

    fun get(): String

    fun clear()
}

class StringBagImpl(
    private val openAi: OpenAi,
) : StringBag {
    private val buffer = StringBuilder()

    override fun add(string: String) {
        assert(string.isNotEmpty())
        buffer.append(string)
        buffer.append("\n")
        openAi.input(buffer.toString())
    }

    override fun get() = buffer.toString()

    override fun clear() {
        buffer.clear()
    }
}
