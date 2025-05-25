import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Date
import java.util.concurrent.TimeUnit
import java.util.logging.Logger

class SpeechKitAuth(
    private val clientId: String,
    private val clientSecret: String,
    private val scope: String = "SALUTE_SPEECH_PERS"
) {
    private val logger = Logger.getLogger(SpeechKitAuth::class.java.name)
    private var accessToken: String? = null
    private var tokenExpirationTime: Long = 0
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    /**
     * Получает действующий токен доступа, при необходимости обновляя его
     */
    @Synchronized
    fun getAccessToken(): String {
        val currentTime = System.currentTimeMillis()

        // Если токен отсутствует или срок его действия истек, получаем новый
        if (accessToken == null || currentTime >= tokenExpirationTime) {
            refreshAccessToken()
        }

        return accessToken ?: throw IllegalStateException("Не удалось получить токен доступа")
    }

    /**
     * Принудительно обновляет токен доступа
     */
    @Synchronized
    fun refreshAccessToken() {
        logger.info("Запрос нового токена доступа...")

        try {
            // Здесь должна быть реализация запроса к API Сбера для получения токена
            // Это примерная реализация, замените на реальный API endpoint и параметры
            val requestBody = """
                {
                    "client_id": "$clientId",
                    "client_secret": "$clientSecret",
                    "scope": "$scope"
                }
            """.trimIndent().toRequestBody()

            val request = Request.Builder()
                .url("https://api.sber.ru/v1/oauth/token")  // Замените на реальный URL
                .post(requestBody)
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Ошибка получения токена: ${response.code} ${response.message}")
                }

                val responseBody = response.body?.string() ?: throw Exception("Пустой ответ")
                val jsonResponse = Json.parseToJsonElement(responseBody).jsonObject

                accessToken = jsonResponse["access_token"]?.toString()?.replace("\"", "")
                val expiresIn = jsonResponse["expires_in"]?.toString()?.toInt() ?: 0

                // Устанавливаем время истечения срока действия токена с запасом в 5 минут
                tokenExpirationTime = System.currentTimeMillis() + (expiresIn - 300) * 1000L

                logger.info("Получен новый токен доступа, действителен до: ${Date(tokenExpirationTime)}")
            }
        } catch (e: Exception) {
            logger.severe("Ошибка при обновлении токена: ${e.message}")
            accessToken = null
            tokenExpirationTime = 0
            throw e
        }
    }
}