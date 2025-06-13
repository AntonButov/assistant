import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId

interface ChatCompletionRequestMapper {
    fun map(string: String): ChatCompletionRequest
}

class ChatCompletionRequestMapperImpl : ChatCompletionRequestMapper {
    override fun map(string: String): ChatCompletionRequest {
        return ChatCompletionRequest(
            model = ModelId("gpt-3.5-turbo"),
            messages =
                listOf(
                    ChatMessage(
                        role = ChatRole.System,
                        content = "You are a helpful assistant that translates English to French.",
                    ),
                    ChatMessage(
                        role = ChatRole.User,
                        content = "Translate the following English text to French: “OpenAI is awesome!”",
                    ),
                ),
        )
    }
}
