import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import tech.antonbutov.api.models.OAuthResponse
import java.util.*
import java.util.logging.Level
import java.util.logging.Logger

class SpeechKitAuth(
    private val authorizationKey: String, // Base64 encoded credentials
    private val scope: String = "SALUTE_SPEECH_PERS"
) {
    private val logger = Logger.getLogger(SpeechKitAuth::class.java.name)
    private var accessToken: String? = null
    private var tokenExpirationTime: Long = 0
    private val httpClient: HttpClient

    init {
        // Создаем HTTP клиент с отключенной проверкой SSL
        httpClient = HttpClient(CIO) {
            // Отключаем проверку SSL для dev/test окружений
            engine {
                https {
                    trustManager = object : javax.net.ssl.X509TrustManager {
                        override fun checkClientTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                        override fun checkServerTrusted(chain: Array<java.security.cert.X509Certificate>, authType: String) {}
                        override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> = arrayOf()
                    }
                }
            }

            // Добавляем поддержку JSON
            install(ContentNegotiation) {
                json(Json {
                    ignoreUnknownKeys = true
                    prettyPrint = true
                    isLenient = true
                })
            }

            // Добавляем логирование
            install(Logging) {
                logger = object : io.ktor.client.plugins.logging.Logger {
                    override fun log(message: String) {
                        Logger.getLogger("Ktor").info(message)
                    }
                }
                level = LogLevel.INFO
            }
        }

        logger.info("SpeechKitAuth создан с использованием Ktor")
    }

    /**
     * Получает действующий токен доступа, при необходимости обновляя его
     */
    @Synchronized
    fun getAccessToken(): String {
        val currentTime = System.currentTimeMillis()
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
            val rquid = UUID.randomUUID().toString()

            // Используем runBlocking для синхронного вызова suspend-функции в Ktor
            val response = runBlocking {
                httpClient.submitForm(
                    url = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth",
                    formParameters = Parameters.build {
                        append("scope", scope)
                    }
                ) {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                        append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        append("RqUID", rquid)
                        append(HttpHeaders.Authorization, "Basic $authorizationKey")
                    }
                }.body<OAuthResponse>()
            }

            accessToken = response.access_token

            // Устанавливаем время истечения срока действия токена с запасом в 5 минут
            tokenExpirationTime = System.currentTimeMillis() + (response.expires_in - 300) * 1000L

            logger.info("Получен новый токен доступа, действителен до: ${Date(tokenExpirationTime)}")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Ошибка при обновлении токена", e)
            accessToken = null
            tokenExpirationTime = 0
            throw e
        }
    }
}