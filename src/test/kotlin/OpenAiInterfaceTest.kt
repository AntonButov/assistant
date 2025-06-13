import com.aallam.openai.api.chat.ChatCompletionChunk
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.client.OpenAI
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test

class OpenAiInterfaceTest {
    @Test
    fun `input should emit text to inputFlow and trigger output`() =
        runTest {
            // Given
            val mockOpenAI = mockk<OpenAI>()
            val mockChatCompletionRequestMapper = mockk<ChatCompletionRequestMapper>()
            val mockChatCompletionMapper = mockk<ChatCompletionMapper>()

            val inputText = "Hello, AI!"
            val expectedRequest = mockk<ChatCompletionRequest>()
            val mockCompletion = mockk<ChatCompletionChunk>()
            val expectedOutput = "Hello, human!"

            every { mockChatCompletionRequestMapper.map(inputText) } returns expectedRequest
            every { mockChatCompletionMapper.map(mockCompletion) } returns expectedOutput
            coEvery { mockOpenAI.chatCompletions(expectedRequest) } returns flowOf(mockCompletion)

            val openAi = OpenAiImpl(mockOpenAI, mockChatCompletionRequestMapper, mockChatCompletionMapper)

            // When
            openAi.input(inputText)

            val result = openAi.output.first()
            assertEquals(expectedOutput, result)

            verify { mockChatCompletionRequestMapper.map(inputText) }
            verify { mockChatCompletionMapper.map(mockCompletion) }
            coVerify { mockOpenAI.chatCompletions(expectedRequest) }
        }

    @Test
    fun `multiple inputs should produce multiple outputs`() =
        runTest {
            // Given
            val mockOpenAI = mockk<OpenAI>()
            val mockChatCompletionRequestMapper = mockk<ChatCompletionRequestMapper>()
            val mockChatCompletionMapper = mockk<ChatCompletionMapper>()

            val input1 = "First message"
            val input2 = "Second message"
            val output1 = "First response"
            val output2 = "Second response"

            val request1 = mockk<ChatCompletionRequest>()
            val request2 = mockk<ChatCompletionRequest>()

            val completion1 = mockk<ChatCompletionChunk>()
            val completion2 = mockk<ChatCompletionChunk>()

            every { mockChatCompletionRequestMapper.map(input1) } returns request1
            every { mockChatCompletionRequestMapper.map(input2) } returns request2
            every { mockChatCompletionMapper.map(completion1) } returns output1
            every { mockChatCompletionMapper.map(completion2) } returns output2
            coEvery { mockOpenAI.chatCompletions(request1) } returns flowOf(completion1)
            coEvery { mockOpenAI.chatCompletions(request2) } returns flowOf(completion2)

            val openAi = OpenAiImpl(mockOpenAI, mockChatCompletionRequestMapper, mockChatCompletionMapper)

            // When & Then
            openAi.input(input1)
            val result1 = openAi.output.first()
            assertEquals(output1, result1)

            openAi.input(input2)
            val result2 = openAi.output.first()
            assertEquals(output2, result2)
        }

    @Test
    fun `output flow should handle empty completion response`() =
        runTest {
            // Given
            val mockOpenAI = mockk<OpenAI>()
            val mockChatCompletionRequestMapper = mockk<ChatCompletionRequestMapper>()
            val mockChatCompletionMapper = mockk<ChatCompletionMapper>()

            val inputText = "Test input"
            val expectedRequest = mockk<ChatCompletionRequest>()
            val mockCompletion = mockk<ChatCompletionChunk>()
            val expectedOutput = ""

            every { mockChatCompletionRequestMapper.map(inputText) } returns expectedRequest
            every { mockChatCompletionMapper.map(mockCompletion) } returns expectedOutput
            coEvery { mockOpenAI.chatCompletions(expectedRequest) } returns flowOf(mockCompletion)

            val openAi = OpenAiImpl(mockOpenAI, mockChatCompletionRequestMapper, mockChatCompletionMapper)

            // When
            openAi.input(inputText)

            // Then
            val result = openAi.output.first()
            assertEquals(expectedOutput, result)
        }

    @Test
    fun `should handle multiple sequential inputs correctly`() =
        runTest {
            // Given
            val mockOpenAI = mockk<OpenAI>()
            val mockChatCompletionRequestMapper = mockk<ChatCompletionRequestMapper>()
            val mockChatCompletionMapper = mockk<ChatCompletionMapper>()

            val inputs = listOf("Input 1", "Input 2", "Input 3")
            val outputs = listOf("Output 1", "Output 2", "Output 3")

            inputs.forEachIndexed { index, input ->
                val request = mockk<ChatCompletionRequest>()
                val completion = mockk<ChatCompletionChunk>()

                every { mockChatCompletionRequestMapper.map(input) } returns request
                every { mockChatCompletionMapper.map(completion) } returns outputs[index]
                coEvery { mockOpenAI.chatCompletions(request) } returns flowOf(completion)
            }

            val openAi = OpenAiImpl(mockOpenAI, mockChatCompletionRequestMapper, mockChatCompletionMapper)

            // When & Then
            inputs.forEachIndexed { index, input ->
                openAi.input(input)
                val result = openAi.output.first()
                assertEquals(outputs[index], result)
            }
        }
}
