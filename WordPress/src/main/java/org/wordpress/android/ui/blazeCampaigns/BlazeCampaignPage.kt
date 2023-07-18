package org.wordpress.android.ui.blazeCampaigns

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.wordpress.android.ui.blazeCampaigns.campaigndetail.CampaignDetailPageSource
import org.wordpress.android.ui.blazeCampaigns.campaignlisting.CampaignListingPageSource

@Parcelize
sealed class BlazeCampaignPage : Parcelable {
    data class CampaignListingPage(val source: CampaignListingPageSource) : BlazeCampaignPage()
    data class CampaignDetailsPage(val source: CampaignDetailPageSource) : BlazeCampaignPage()
    object Done: BlazeCampaignPage()
}
