import java.util.logging.Logger

object LoggerAssistant {
    private val logger = Logger.getLogger("SpeechKit")
    fun info(message: String) {
        logger.info(message)
    }
}
