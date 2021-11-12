package org.wordpress.android.fluxc.network.rest.wpcom.dashboard

import android.content.Context
import com.android.volley.RequestQueue
import com.google.gson.annotations.SerializedName
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardsModel
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpcom.BaseWPComRestClient
import org.wordpress.android.fluxc.network.rest.wpcom.auth.AccessToken
import org.wordpress.android.fluxc.store.dashboard.CardsStore.FetchedCardsPayload
import java.util.Date
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class CardsRestClient @Inject constructor(
    dispatcher: Dispatcher,
    appContext: Context?,
    @Named("regular") requestQueue: RequestQueue,
    accessToken: AccessToken,
    userAgent: UserAgent
) : BaseWPComRestClient(appContext, dispatcher, requestQueue, accessToken, userAgent) {
    @Suppress("RedundantSuspendModifier", "UNUSED_PARAMETER")
    suspend fun fetchCards(site: SiteModel) = FetchedCardsPayload(
            CardsResponse(
                    posts = PostsResponse(
                            hasPublished = false,
                            draft = listOf(),
                            scheduled = listOf()
                    )
            )
    )

    data class CardsResponse(
        @SerializedName("posts") val posts: PostsResponse
    ) {
        fun toCards(): CardsModel = CardsModel(
                posts = posts.toPosts()
        )
    }

    data class PostsResponse(
        @SerializedName("has_published") val hasPublished: Boolean,
        @SerializedName("draft") val draft: List<PostResponse>,
        @SerializedName("scheduled") val scheduled: List<PostResponse>
    ) {
        fun toPosts(): CardsModel.PostsModel = CardsModel.PostsModel(
                hasPublished = hasPublished,
                draft = draft.map { it.toPost() },
                scheduled = scheduled.map { it.toPost() }
        )
    }

    data class PostResponse(
        @SerializedName("ID") val id: Int,
        @SerializedName("post_title") val title: String?,
        @SerializedName("post_content") val content: String?,
        @SerializedName("post_modified") val date: Date,
        @SerializedName("featured_image") val featuredImage: String?
    ) {
        fun toPost() = CardsModel.PostsModel.PostModel(
                id = id,
                title = title,
                content = content,
                date = date,
                featuredImage = featuredImage
        )
    }
}
