import androidx.compose.runtime.State
import com.aallam.openai.api.http.Timeout
import com.aallam.openai.client.OpenAI
import kotlin.time.Duration.Companion.seconds

interface KrosAiInterface {
    val output: androidx.compose.runtime.State<String>
    fun input(text: String)
}

class KrosAi(): KrosAiInterface {
    val openai = OpenAI(
        token = "your-api-key",
        timeout = Timeout(socket = 60.seconds),
    )
    override val output: State<String>
        get() = TODO("Not yet implemented")

    override fun input(text: String) {
        TODO("Not yet implemented")
    }


}