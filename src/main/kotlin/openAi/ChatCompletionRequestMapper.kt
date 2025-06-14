package openAi

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId

interface ChatCompletionRequestMapper {
    fun map(string: String): ChatCompletionRequest
}

private const val PROMPT_REVIEW = "Я прохожу собеседование. Мне нужны короткие подсказки по ходу беседы."
private const val PROMPT_CHECK = "Я проверяю настройки. Отвечай то что получаешь, пож-та."

class ChatCompletionRequestMapperImpl : ChatCompletionRequestMapper {
    override fun map(string: String): ChatCompletionRequest {
        return ChatCompletionRequest(
            model = ModelId("gpt-3.5-turbo"),
            messages =
                listOf(
                    ChatMessage(
                        role = ChatRole.System,
                        content = PROMPT_CHECK,
                    ),
                    ChatMessage(
                        role = ChatRole.User,
                        content = string,
                    ),
                ),
        )
    }
}
