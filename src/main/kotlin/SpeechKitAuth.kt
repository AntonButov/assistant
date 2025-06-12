import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.forms.submitForm
import io.ktor.client.request.headers
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.Parameters
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import java.security.cert.X509Certificate
import java.util.UUID
import java.util.logging.Level
import java.util.logging.Logger
import javax.net.ssl.X509TrustManager

class SpeechKitAuth {
    val authorizationKey = PropertyLoader.Companion.getProperty("speechkit.authorization.key")
    val scope = "SALUTE_SPEECH_PERS"

    private val logger = Logger.getLogger(SpeechKitAuth::class.java.name)
    private var accessToken: String? = null
    private var tokenExpirationTime: Long = 0

    // Создаем HTTP клиент с отключенной проверкой SSL
    private val httpClient: HttpClient =
        HttpClient(CIO) {
            // Отключаем проверку SSL для dev/test окружений
            engine {
                https {
                    trustManager =
                        object : X509TrustManager {
                            override fun checkClientTrusted(
                                chain: Array<X509Certificate>,
                                authType: String,
                            ) {
                            }

                            override fun checkServerTrusted(
                                chain: Array<X509Certificate>,
                                authType: String,
                            ) {
                            }

                            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                        }
                }
            }

            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                        isLenient = true
                    },
                )
            }

            install(Logging) {
                logger =
                    object : io.ktor.client.plugins.logging.Logger {
                        override fun log(message: String) {
                            Logger.getLogger("Ktor").info(message)
                        }
                    }
                level = LogLevel.INFO
            }
        }

    suspend fun getAccessToken(): String {
        val currentTime = System.currentTimeMillis()
        if (accessToken == null || currentTime >= tokenExpirationTime) {
            refreshAccessToken()
        }
        return accessToken ?: throw IllegalStateException("Не удалось получить токен доступа")
    }

    suspend fun refreshAccessToken() {
        try {
            val rquid = UUID.randomUUID().toString()

            val response =
                httpClient.submitForm(
                    url = "https://ngw.devices.sberbank.ru:9443/api/v2/oauth",
                    formParameters =
                        Parameters.build {
                            append("scope", scope)
                        },
                ) {
                    headers {
                        append(HttpHeaders.ContentType, ContentType.Application.FormUrlEncoded.toString())
                        append(HttpHeaders.Accept, ContentType.Application.Json.toString())
                        append("RqUID", rquid)
                        append(HttpHeaders.Authorization, "Basic $authorizationKey")
                    }
                }.body<OAuthResponse>()

            accessToken = response.accessToken

            // Устанавливаем время истечения срока действия токена с запасом в 5 минут
            tokenExpirationTime = System.currentTimeMillis() + (response.expiresAt - 300) * 1000L

            // logger.info("Получен новый токен доступа, действителен до: ${Date(tokenExpirationTime)}")
        } catch (e: Exception) {
            logger.log(Level.SEVERE, "Ошибка при обновлении токена", e)
            accessToken = null
            tokenExpirationTime = 0
            throw e
        }
    }
}
