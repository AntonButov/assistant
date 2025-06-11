import kotlinx.serialization.Serializable

@Serializable
data class OAuthResponse(
    val accessToken: String,
    val expiresAt: Long,
)