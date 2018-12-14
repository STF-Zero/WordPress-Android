package org.wordpress.android.fluxc.model.stats.time

import com.google.gson.Gson
import org.wordpress.android.fluxc.model.stats.time.ClicksModel.Click
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsModel
import org.wordpress.android.fluxc.model.stats.time.PostAndPageViewsModel.ViewsType
import org.wordpress.android.fluxc.model.stats.time.ReferrersModel.Referrer
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ClicksRestClient.ClicksResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.PostAndPageViewsRestClient.PostAndPageViewsResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.ReferrersRestClient.ReferrersResponse
import org.wordpress.android.fluxc.network.rest.wpcom.stats.time.SearchTermsRestClient.SearchTermsResponse
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T.STATS
import javax.inject.Inject

class TimeStatsMapper
@Inject constructor(val gson: Gson) {
    fun map(response: PostAndPageViewsResponse, pageSize: Int): PostAndPageViewsModel {
        val postViews = response.days.entries.first().value.postViews
        val stats = postViews.take(pageSize).mapNotNull { item ->
            val type = when (item.type) {
                "post" -> ViewsType.POST
                "page" -> ViewsType.PAGE
                "homepage" -> ViewsType.HOMEPAGE
                else -> {
                    AppLog.e(STATS, "PostAndPageViewsResponse.type: Unexpected view type: ${item.type}")
                    null
                }
            }
            type?.let {
                if (item.id == null || item.title == null || item.href == null) {
                    AppLog.e(STATS, "PostAndPageViewsResponse.type: Non-nullable fields are null - $item")
                }
                ViewsModel(item.id ?: 0, item.title ?: "", item.views ?: 0, type, item.href ?: "")
            }
        }
        return PostAndPageViewsModel(stats, postViews.size > pageSize)
    }

    fun map(response: ReferrersResponse, pageSize: Int): ReferrersModel {
        val first = response.groups.values.first()
        val groups = first.groups.take(pageSize).map { group ->
            val children = group.referrers?.mapNotNull { result ->
                if (result.name != null && result.views != null) {
                    val firstChildUrl = result.children?.firstOrNull()?.url
                    Referrer(result.name, result.views, result.icon, firstChildUrl ?: result.url)
                } else {
                    AppLog.e(STATS, "ReferrersResponse.type: Missing fields on a referrer")
                    null
                }
            }
            ReferrersModel.Group(group.groupId, group.name, group.icon, group.url, group.total, children ?: listOf())
        }
        return ReferrersModel(first.otherViews ?: 0, first.totalViews ?: 0, groups, first.groups.size > groups.size)
    }

    fun map(response: ClicksResponse, pageSize: Int): ClicksModel {
        val first = response.groups.values.first()
        val groups = first.clicks.take(pageSize).map { group ->
            val children = group.clicks?.mapNotNull { result ->
                if (result.name != null && result.views != null) {
                    Click(result.name, result.views, result.icon, result.url)
                } else {
                    AppLog.e(STATS, "ClicksResponse.type: Missing fields on a Click object")
                    null
                }
            }
            ClicksModel.Group(group.groupId, group.name, group.icon, group.url, group.views, children ?: listOf())
        }
        return ClicksModel(
                first.otherClicks ?: 0,
                first.totalClicks ?: 0,
                groups,
                first.clicks.size > groups.size
        )
    }

    fun map(response: SearchTermsResponse, pageSize: Int): SearchTermsModel {
        val first = response.days.values.first()
        val groups = first.searchTerms.mapNotNull { searchTerm ->
            if (searchTerm.term != null) {
                SearchTermsModel.SearchTerm(searchTerm.term, searchTerm.views ?: 0)
            } else {
                AppLog.e(STATS, "ClicksResponse.type: Missing fields on a Click object")
                null
            }
        }.take(pageSize)
        return SearchTermsModel(
                first.otherSearchTerms ?: 0,
                first.totalSearchTimes ?: 0,
                groups,
                first.searchTerms.size > groups.size
        )
    }
}
