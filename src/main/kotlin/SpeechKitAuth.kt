import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Date
import java.util.UUID
import java.util.concurrent.TimeUnit
import java.util.logging.Logger
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class SpeechKitAuth(
    private val authorizationKey: String, // Base64 encoded credentials
    private val scope: String = "SALUTE_SPEECH_PERS"
) {
    private val logger = Logger.getLogger(SpeechKitAuth::class.java.name)
    private var accessToken: String? = null
    private var tokenExpirationTime: Long = 0
    private val httpClient: OkHttpClient

    init {
        // Создаем TrustManager, который принимает любые сертификаты
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager {
                override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
                override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
            }
        )

        // Создаем SSLContext с нашим TrustManager
        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, trustAllCerts, SecureRandom())

        // Создаем OkHttpClient с отключенной проверкой сертификатов
        httpClient = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
            .hostnameVerifier { _, _ -> true } // Отключаем проверку имени хоста
            .build()

        logger.info("SpeechKitAuth создан с отключенной проверкой SSL-сертификатов")
    }

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
            // Формируем тело запроса согласно curl-примеру
            val formBody = FormBody.Builder()
                .add("scope", scope)
                .build()

            // Генерируем уникальный RqUID
            val rquid = UUID.randomUUID().toString()

            // Создаем запрос согласно curl-примеру
            val request = Request.Builder()
                .url("https://ngw.devices.sberbank.ru:9443/api/v2/oauth")
                .post(formBody)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Accept", "application/json")
                .header("RqUID", rquid)
                .header("Authorization", "Basic $authorizationKey")
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw Exception("Ошибка получения токена: ${response.code} ${response.message}")
                }

                val responseBody = response.body?.string() ?: throw Exception("Пустой ответ")

                // Парсинг JSON без использования внешних библиотек
                val accessTokenRegex = "\"access_token\"\\s*:\\s*\"([^\"]+)\"".toRegex()
                val expiresInRegex = "\"expires_in\"\\s*:\\s*(\\d+)".toRegex()

                accessToken = accessTokenRegex.find(responseBody)?.groupValues?.get(1)
                    ?: throw Exception("access_token не найден в ответе")

                val expiresIn = expiresInRegex.find(responseBody)?.groupValues?.get(1)?.toIntOrNull()
                    ?: throw Exception("expires_in не найден в ответе")

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