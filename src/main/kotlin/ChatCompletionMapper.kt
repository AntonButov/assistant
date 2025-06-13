import com.aallam.openai.api.chat.ChatCompletionChunk

interface ChatCompletionMapper {
    fun map(chatCompletion: ChatCompletionChunk): String
}

class ChatCompletionMapperImpl : ChatCompletionMapper {
    override fun map(chatCompletion: ChatCompletionChunk): String {
        return chatCompletion.choices.first().delta.content.orEmpty()
    }
}
