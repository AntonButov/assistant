package openAi

import com.aallam.openai.api.chat.ChatCompletionRequest
import com.aallam.openai.api.chat.ChatMessage
import com.aallam.openai.api.chat.ChatRole
import com.aallam.openai.api.model.ModelId

interface ChatCompletionRequestMapper {
    fun map(string: String): ChatCompletionRequest
}

private const val PROMPT_SUMMARIZE = "Ты слушаешь поток аудио. И составляешь целые разумные предложения."
private const val PROMPT_REVIEW = "Ты проходишь собеседование на Андроид-разработчика. Ты опытный андроид разработчик. Давай четкие ответы на вопросы, которые я задаю. Возможно ты не будешь все слышать догадывайся. Не пиши лишнего текста."
private const val PROMPT_CHECK = "Я проверяю настройки. Отвечай то что получаешь, пож-та. "

class ChatCompletionRequestMapperImpl : ChatCompletionRequestMapper {
    override fun map(string: String): ChatCompletionRequest {
        return ChatCompletionRequest(
            model = ModelId("gpt-3.5-turbo"),
            messages =
                listOf(
                    ChatMessage(
                        role = ChatRole.System,
                        content = PROMPT_SUMMARIZE,
                    ),
                    ChatMessage(
                        role = ChatRole.User,
                        content = string,
                    ),
                ),
        )
    }
}
