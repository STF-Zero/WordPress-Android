package org.wordpress.android.fluxc.store.jetpackai

import org.wordpress.android.fluxc.model.JWTToken
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAICompletionsErrorType.AUTH_ERROR
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAICompletionsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAIJWTTokenResponse
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAIJWTTokenResponse.Error
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.JetpackAIJWTTokenResponse.Success
import org.wordpress.android.fluxc.network.rest.wpcom.jetpackai.JetpackAIRestClient.ResponseFormat
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class JetpackAIStore @Inject constructor(
    private val jetpackAIRestClient: JetpackAIRestClient,
    private val coroutineEngine: CoroutineEngine
) {
    private var token: JWTToken? = null

    /**
     * Fetches Jetpack AI completions for a given prompt to be used on a particular post.
     *
     * @param site      The site for which completions are fetched.
     * @param prompt    The prompt used to generate completions.
     * @param skipCache If true, bypasses the default 30-second throttle and fetches fresh data.
     * @param feature   Used by backend to track AI-generation usage and measure costs. Optional.
     * @param postId    Used to mark the post as having content generated by Jetpack AI.
     */
    suspend fun fetchJetpackAICompletionsForPost(
        site: SiteModel,
        prompt: String,
        postId: Long,
        feature: String,
        skipCache: Boolean = false
    ) = fetchJetpackAICompletions(site, prompt, feature, skipCache, postId)


    /**
     * Fetches Jetpack AI completions for a given prompt used globally by a site.
     *
     * @param site      The site for which completions are fetched.
     * @param prompt    The prompt used to generate completions.
     * @param feature   Used by backend to track AI-generation usage and measure costs. Optional.
     * @param skipCache If true, bypasses the default 30-second throttle and fetches fresh data.
     */
    suspend fun fetchJetpackAICompletionsForSite(
        site: SiteModel,
        prompt: String,
        feature: String? = null,
        skipCache: Boolean = false
    ) = fetchJetpackAICompletions(site, prompt, feature, skipCache)

    private suspend fun fetchJetpackAICompletions(
        site: SiteModel,
        prompt: String,
        feature: String? = null,
        skipCache: Boolean,
        postId: Long? = null
    ) = coroutineEngine.withDefaultContext(
        tag = AppLog.T.API,
        caller = this,
        loggedMessage = "fetch Jetpack AI completions"
    ) {
        jetpackAIRestClient.fetchJetpackAICompletions(site, prompt, feature, skipCache, postId)
    }

    suspend fun fetchJetpackAICompletions(
        site: SiteModel,
        prompt: String,
        feature: String,
        responseFormat: ResponseFormat? = null,
        model: String? = null
    ): JetpackAICompletionsResponse = coroutineEngine.withDefaultContext(
        tag = AppLog.T.API,
        caller = this,
        loggedMessage = "fetch Jetpack AI completions"
    ) {
        val token = token?.validateExpiryDate()?.validateBlogId(site.siteId)
            ?: fetchJetpackAIJWTToken(site).let { tokenResponse ->
                when (tokenResponse) {
                    is Error -> {
                        return@withDefaultContext JetpackAICompletionsResponse.Error(
                            type = AUTH_ERROR,
                            message = tokenResponse.message,
                        )
                    }

                    is Success -> {
                        token = tokenResponse.token
                        tokenResponse.token
                    }
                }
            }

        val result = jetpackAIRestClient.fetchJetpackAITextCompletion(
            token,
            prompt,
            feature,
            responseFormat,
            model
        )

        return@withDefaultContext when {
            // Fetch token anew if using existing token returns AUTH_ERROR
            result is JetpackAICompletionsResponse.Error && result.type == AUTH_ERROR -> {
                // Remove cached token
                this@JetpackAIStore.token = null
                fetchJetpackAICompletions(site, prompt, feature, responseFormat, model)
            }

            else -> result
        }
    }

    private suspend fun fetchJetpackAIJWTToken(site: SiteModel): JetpackAIJWTTokenResponse =
        coroutineEngine.withDefaultContext(
            tag = AppLog.T.API,
            caller = this,
            loggedMessage = "fetch Jetpack AI JWT token"
        ) {
            jetpackAIRestClient.fetchJetpackAIJWTToken(site)
        }

    private fun JWTToken.validateBlogId(blogId: Long): JWTToken? =
        if (getPayloadItem("blog_id")?.toLong() == blogId) this else null
}
