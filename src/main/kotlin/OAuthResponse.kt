import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OAuthResponse(
    @SerialName("access_token")
    val accessToken: String,
    @SerialName("expires_at")
    val expiresAt: Long,)