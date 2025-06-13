import junit.framework.TestCase.assertEquals
import org.junit.Test
import org.junit.jupiter.api.assertThrows

class StringBufferTest {
    @Test
    fun `add should append string with newline`() {
        val buffer = StringBagImpl()
        buffer.add("Hello")
        buffer.add("World")
        assertEquals("Hello\nWorld\n", buffer.get())
    }

    @Test
    fun `add should throw assertion error on empty string`() {
        val buffer = StringBagImpl()
        assertThrows<AssertionError> {
            buffer.add("")
        }
    }

    @Test
    fun `get should return empty string if nothing added`() {
        val buffer = StringBagImpl()
        assertEquals("", buffer.get())
    }
}
