package openAi

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId

interface ChatCompletionRequestMapper {
    fun map(string: String): ChatCompletionRequest
}

private const val PROMPT = "Я прохожу собеседование. Мне нужны короткие подсказки по ходу беседы."

class ChatCompletionRequestMapperImpl : ChatCompletionRequestMapper {
    override fun map(string: String): ChatCompletionRequest {
        return ChatCompletionRequest(
            model = ModelId("gpt-3.5-turbo"),
            messages =
                listOf(
                    ChatMessage(
                        role = ChatRole.System,
                        content = PROMPT,
                    ),
                    ChatMessage(
                        role = ChatRole.User,
                        content = string,
                    ),
                ),
        )
    }
}
