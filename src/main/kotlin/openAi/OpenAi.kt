package openAi

import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import tools.LoggerAssistant

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
            val request = chatCompletionRequestMapper.map(input)
            LoggerAssistant.info("request = $request")
            openAi.chatCompletions(request)
                .map {
                    chatCompletionMapper.map(it).also {
                        LoggerAssistant.info("From gpt: $it")
                    }
                }
        }

    override fun input(text: String) {
        LoggerAssistant.info("input = $text")
        inputFlow.tryEmit(text)
    }
}
