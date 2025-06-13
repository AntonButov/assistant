interface StringBag {
    fun add(string: String)

    fun get(): String

    fun clear()

    fun isEmpty(): Boolean

    fun size(): Int
}

class StringBagImpl : StringBag {
    private val buffer = StringBuilder()
    private var lineCount = 0

    override fun add(string: String) {
        assert(string.isNotEmpty())
        buffer.append(string)
        buffer.append("\n")
        lineCount++
    }

    override fun get() = buffer.toString()

    override fun clear() {
        buffer.clear()
        lineCount = 0
    }

    override fun isEmpty(): Boolean = buffer.isEmpty()

    override fun size(): Int = lineCount
}
