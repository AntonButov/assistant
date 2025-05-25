package tech.antonbutov.api

import de.jensklingenberg.ktorfit.http.Body
import de.jensklingenberg.ktorfit.http.Field
import de.jensklingenberg.ktorfit.http.FormUrlEncoded
import de.jensklingenberg.ktorfit.http.Header
import de.jensklingenberg.ktorfit.http.Headers
import de.jensklingenberg.ktorfit.http.POST
import tech.antonbutov.api.models.OAuthResponse

interface SberApi {
    @FormUrlEncoded
    @POST("/api/v2/oauth")
    @Headers("Accept: application/json")
    suspend fun getOAuthToken(
        @Header("RqUID") rquid: String,
        @Header("Authorization") authorization: String,
        @Field("scope") scope: String
    ): OAuthResponse
}