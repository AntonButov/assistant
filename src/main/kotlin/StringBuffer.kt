

class StringBuffer {
    private val buffer = StringBuilder()
    fun add(string: String) {
            assert(string.isNotEmpty())
            buffer.append(string)
            buffer.append("\n")
    }
    fun get() = buffer.toString()
}