import io.mockk.mockk
import junit.framework.TestCase.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.jupiter.api.assertThrows

class StringBufferTest {
    private lateinit var buffer: StringBagImpl

    @Before
    fun setUp() {
        buffer = StringBagImpl(mockk(relaxed = true))
    }

    @Test
    fun `add should append string with newline`() {
        buffer.add("Hello")
        buffer.add("World")
        assertEquals("Hello\nWorld\n", buffer.get())
    }

    @Test
    fun `add should throw assertion error on empty string`() {
        assertThrows<AssertionError> {
            buffer.add("")
        }
    }

    @Test
    fun `get should return empty string if nothing added`() {
        assertEquals("", buffer.get())
    }
}
