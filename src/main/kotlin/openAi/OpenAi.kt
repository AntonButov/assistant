package openAi

import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

interface OpenAi {
    val output: Flow<String?> // переделать на sealed

    fun input(text: String)
}

class OpenAiImpl(
    private val openAi: OpenAI,
    private val chatCompletionRequestMapper: ChatCompletionRequestMapper,
    private val chatCompletionMapper: ChatCompletionMapper,
) : OpenAi {
    private val inputFlow = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val output: Flow<String?> =
        inputFlow.flatMapLatest { input ->
            input ?: return@flatMapLatest flowOf(null)
            openAi.chatCompletions(chatCompletionRequestMapper.map(input))
                .map {
                    chatCompletionMapper.map(it)
                }
        }

    override fun input(text: String) {
        inputFlow.tryEmit(text)
    }
}
