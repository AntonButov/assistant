package tech.antonbutov.api.models

import kotlinx.serialization.Serializable

@Serializable
data class OAuthResponse(
    val access_token: String,
    val expires_in: Int,
    val token_type: String = "Bearer"
)