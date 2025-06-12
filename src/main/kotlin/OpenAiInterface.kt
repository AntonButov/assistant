import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId
import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.onEach

interface OpenAiInterface {
    val output: Flow<String>
    fun input(text: String)
}

class OpenAi(private val openAi: OpenAI): OpenAiInterface {
    private val inputFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val output: Flow<String> = inputFlow.flatMapLatest { it ->
        openAi.chatCompletions(it.toChatCompletionRequest())
            .map {
                it.choices.first().delta.content.orEmpty()
            }

    }

    override fun input(text: String) {
       inputFlow.tryEmit(text)
    }

    private fun String.toChatCompletionRequest() = ChatCompletionRequest(
        model = ModelId("gpt-3.5-turbo"),
        messages = listOf(
            ChatMessage(
                role = ChatRole.System,
                content = "You are a helpful assistant that translates English to French."
            ),
            ChatMessage(
                role = ChatRole.User,
                content = "Translate the following English text to French: “OpenAI is awesome!”"
            )
        )
    )

}