package openAi

import com.aallam.openai.client.OpenAI
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import `resul-text`.ResultClean
import `resul-text`.ResultState
import `resul-text`.ResultStateText
import tools.LoggerAssistant

interface OpenAi {
    val output: Flow<ResultState?> // убрать null

    fun input(text: String)
}

class OpenAiImpl(
    private val openAi: OpenAI,
    private val chatCompletionRequestMapper: ChatCompletionRequestMapper,
    private val chatCompletionMapper: ChatCompletionMapper,
) : OpenAi {
    private val inputFlow = MutableStateFlow<String?>(null)

    @OptIn(ExperimentalCoroutinesApi::class)
    override val output: Flow<ResultState?> =
        inputFlow.flatMapLatest { input ->
            input ?: return@flatMapLatest flowOf(null)
            val request = chatCompletionRequestMapper.map(input)
            LoggerAssistant.info("${input} ----------->>")
            merge(
                flowOf(ResultClean),
                openAi.chatCompletions(request)
                    .map {
                        ResultStateText(
                            chatCompletionMapper.map(it)
                        )
                    }
            )

        }

    override fun input(text: String) {
        inputFlow.tryEmit(text)
    }
}
