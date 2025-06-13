import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map

interface OpenAiInterface {
    val output: Flow<String>

    fun input(text: String)
}

class OpenAi(
    private val openAi: OpenAI,
    private val chatCompletionRequestMapper: ChatCompletionRequestMapper,
    private val chatCompletionMapper: ChatCompletionMapper,
) : OpenAiInterface {
    private val inputFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val output: Flow<String> =
        inputFlow.flatMapLatest { it ->
            openAi.chatCompletions(chatCompletionRequestMapper.map(it))
                .map {
                    chatCompletionMapper.map(it)
                }
        }

    override fun input(text: String) {
        inputFlow.tryEmit(text)
    }
}
