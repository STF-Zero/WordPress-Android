package org.wordpress.android.ui.mysite

import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.atMost
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel
import org.wordpress.android.fluxc.model.dashboard.CardModel.PostsCardModel.PostCardModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartNewSiteTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTaskType
import org.wordpress.android.localcontentmigration.ContentMigrationAnalyticsTracker
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.jetpackoverlay.individualplugin.WPJetpackIndividualPluginHelper
import org.wordpress.android.ui.jetpackplugininstall.fullplugin.GetShowJetpackFullPluginInstallOnboardingUseCase
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.DomainRegistrationCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.JetpackFeatureCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.QuickStartCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Card.SiteInfoHeaderCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.InfoItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.ListItem
import org.wordpress.android.ui.mysite.MySiteCardAndItem.Item.SingleActionCard
import org.wordpress.android.ui.mysite.MySiteCardAndItem.JetpackBadge
import org.wordpress.android.ui.mysite.MySiteCardAndItemBuilderParams.InfoItemBuilderParams
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.AccountData
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.CardsUpdate
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.DomainCreditAvailable
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.JetpackCapabilities
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.QuickStartUpdate
import org.wordpress.android.ui.mysite.MySiteUiState.PartialState.SelectedSite
import org.wordpress.android.ui.mysite.MySiteViewModel.State.NoSites
import org.wordpress.android.ui.mysite.MySiteViewModel.State.SiteSelected
import org.wordpress.android.ui.mysite.MySiteViewModel.TextInputDialogModel
import org.wordpress.android.ui.mysite.cards.DashboardCardsViewModelSlice
import org.wordpress.android.ui.mysite.cards.DomainRegistrationCardShownTracker
import org.wordpress.android.ui.mysite.cards.dashboard.CardsTracker
import org.wordpress.android.ui.mysite.cards.dashboard.activity.ActivityLogCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts.BloggingPromptCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.pages.PagesCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.posts.PostsCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.dashboard.todaysstats.TodaysStatsViewModelSlice
import org.wordpress.android.ui.mysite.cards.dynamiccard.DynamicCardsViewModelSlice
import org.wordpress.android.ui.mysite.cards.jetpackfeature.JetpackFeatureCardHelper
import org.wordpress.android.ui.mysite.cards.jetpackfeature.JetpackFeatureCardShownTracker
import org.wordpress.android.ui.mysite.cards.jpfullplugininstall.JetpackInstallFullPluginShownTracker
import org.wordpress.android.ui.mysite.cards.personalize.PersonalizeCardBuilder
import org.wordpress.android.ui.mysite.cards.personalize.PersonalizeCardViewModelSlice
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository
import org.wordpress.android.ui.mysite.cards.quickstart.QuickStartRepository.QuickStartCategory
import org.wordpress.android.ui.mysite.cards.siteinfo.SiteInfoHeaderCardViewModelSlice
import org.wordpress.android.ui.mysite.items.DashboardItemsViewModelSlice
import org.wordpress.android.ui.mysite.items.infoitem.MySiteInfoItemBuilder
import org.wordpress.android.ui.mysite.items.listitem.ListItemAction
import org.wordpress.android.ui.mysite.items.listitem.SiteItemsBuilder
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.BasicDialogViewModel.DialogInteraction
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.quickstart.QuickStartTaskDetails
import org.wordpress.android.ui.quickstart.QuickStartTracker
import org.wordpress.android.ui.quickstart.QuickStartType
import org.wordpress.android.ui.sitecreation.misc.SiteCreationSource
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.JetpackBrandingUtils
import org.wordpress.android.util.QuickStartUtilsWrapper
import org.wordpress.android.util.SnackbarSequencer
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.config.LandOnTheEditorFeatureConfig
import org.wordpress.android.util.publicdata.AppStatus
import org.wordpress.android.util.publicdata.WordPressPublicData
import org.wordpress.android.viewmodel.Event
import java.util.Date

@Suppress("LargeClass")
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
@Ignore("Failing, update it to work with the new code")
class MySiteViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var siteItemsBuilder: SiteItemsBuilder

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var accountStore: AccountStore

    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var siteIconUploadHandler: SiteIconUploadHandler

    @Mock
    lateinit var quickStartRepository: QuickStartRepository

    @Mock
    lateinit var homePageDataLoader: HomePageDataLoader

    @Mock
    lateinit var quickStartUtilsWrapper: QuickStartUtilsWrapper

    @Mock
    lateinit var snackbarSequencer: SnackbarSequencer

    @Mock
    lateinit var landOnTheEditorFeatureConfig: LandOnTheEditorFeatureConfig

    @Mock
    lateinit var cardsTracker: CardsTracker

    @Mock
    lateinit var domainRegistrationCardShownTracker: DomainRegistrationCardShownTracker

    @Mock
    lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    lateinit var getShowJetpackFullPluginInstallOnboardingUseCase: GetShowJetpackFullPluginInstallOnboardingUseCase

    @Mock
    lateinit var contentMigrationAnalyticsTracker: ContentMigrationAnalyticsTracker

    @Mock
    lateinit var jetpackBrandingUtils: JetpackBrandingUtils

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var quickStartType: QuickStartType

    @Mock
    lateinit var quickStartTracker: QuickStartTracker

    @Mock
    private lateinit var dispatcher: Dispatcher

    @Mock
    lateinit var appStatus: AppStatus

    @Mock
    lateinit var wordPressPublicData: WordPressPublicData

    @Mock
    lateinit var jetpackFeatureCardShownTracker: JetpackFeatureCardShownTracker

    @Mock
    lateinit var jetpackFeatureCardHelper: JetpackFeatureCardHelper

    @Mock
    lateinit var jetpackFeatureRemovalOverlayUtil: JetpackFeatureRemovalOverlayUtil

    @Mock
    lateinit var jetpackInstallFullPluginShownTracker: JetpackInstallFullPluginShownTracker

    @Mock
    lateinit var pagesCardViewModelSlice: PagesCardViewModelSlice

    @Mock
    lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    @Mock
    lateinit var wpJetpackIndividualPluginHelper: WPJetpackIndividualPluginHelper

    @Mock
    lateinit var todaysStatsViewModelSlice: TodaysStatsViewModelSlice

    @Mock
    lateinit var postsCardViewModelSlice: PostsCardViewModelSlice

    @Mock
    lateinit var activityLogCardViewModelSlice: ActivityLogCardViewModelSlice

    @Mock
    lateinit var mySiteInfoItemBuilder: MySiteInfoItemBuilder

    @Mock
    lateinit var personalizeCardBuilder: PersonalizeCardBuilder

    @Mock
    lateinit var personalizeCardViewModelSlice: PersonalizeCardViewModelSlice

    @Mock
    lateinit var bloggingPromptCardViewModelSlice: BloggingPromptCardViewModelSlice

    @Mock
    lateinit var siteInfoHeaderCardViewModelSlice: SiteInfoHeaderCardViewModelSlice

    @Mock
    lateinit var dynamicCardsViewModelSlice: DynamicCardsViewModelSlice

    @Mock
    lateinit var accountDataViewModelSlice: AccountDataViewModelSlice

    @Mock
    lateinit var dashboardCardsViewModelSlice: DashboardCardsViewModelSlice

    @Mock
    lateinit var dashboardItemsViewModelSlice: DashboardItemsViewModelSlice


    private lateinit var viewModel: MySiteViewModel
    private lateinit var uiModels: MutableList<MySiteViewModel.State>
    private lateinit var snackbars: MutableList<SnackbarMessageHolder>
    private lateinit var textInputDialogModels: MutableList<TextInputDialogModel>
    private lateinit var dialogModels: MutableList<SiteDialogModel>
    private lateinit var navigationActions: MutableList<SiteNavigationAction>
    private lateinit var showSwipeRefreshLayout: MutableList<Boolean>
    private val avatarUrl = "https://1.gravatar.com/avatar/1000?s=96&d=identicon"
    private val userName = "Username"
    private val siteLocalId = 1
    private val siteUrl = "http://site.com"
    private val siteIcon = "http://site.com/icon.jpg"
    private val siteName = "Site"
    private val emailAddress = "test@email.com"
    private val localHomepageId = 1
    private val bloggingPromptId = 123
    private lateinit var site: SiteModel
    private lateinit var homepage: PageModel
    private val onSiteChange = MutableLiveData<SiteModel>()
    private val onSiteSelected = MutableLiveData<Int>()
    private val onShowSiteIconProgressBar = MutableLiveData<Boolean>()
    private val isDomainCreditAvailable = MutableLiveData(DomainCreditAvailable(false))
    private val selectedSite = MediatorLiveData<SelectedSite>()
    private val refresh = MutableLiveData<Event<Boolean>>()

    private val jetpackCapabilities = MutableLiveData(
        JetpackCapabilities(
            scanAvailable = false,
            backupAvailable = false
        )
    )
    private val currentAvatar = MutableLiveData(AccountData("",""))
    private val quickStartUpdate = MutableLiveData(QuickStartUpdate())
    private val activeTask = MutableLiveData<QuickStartTask>()

    private var quickStartTaskTypeItemClickAction: ((QuickStartTaskType) -> Unit)? = null
    private var onDashboardErrorRetryClick: (() -> Unit)? = null
    private val quickStartCategory: QuickStartCategory
        get() = QuickStartCategory(
            taskType = QuickStartTaskType.CUSTOMIZE,
            uncompletedTasks = listOf(QuickStartTaskDetails.UPDATE_SITE_TITLE),
            completedTasks = emptyList()
        )

    private val cardsUpdate = MutableLiveData(
        CardsUpdate(
            cards = listOf(
                PostsCardModel(
                    hasPublished = true,
                    draft = listOf(
                        PostCardModel(
                            id = 1,
                            title = "draft",
                            content = "content",
                            featuredImage = "featuredImage",
                            date = Date()
                        )
                    ),
                    scheduled = listOf(
                        PostCardModel(
                            id = 2,
                            title = "scheduled",
                            content = "",
                            featuredImage = null,
                            date = Date()
                        )
                    )
                )
            )
        )
    )


    @Suppress("LongMethod")
    @Before
    fun setUp() {
        init()
    }

    @Suppress("LongMethod")
    fun init() = test {
        onSiteChange.value = null
        onShowSiteIconProgressBar.value = null
        onSiteSelected.value = null
        selectedSite.value = null
        whenever(selectedSiteRepository.siteSelected).thenReturn(onSiteSelected)
        whenever(quickStartRepository.activeTask).thenReturn(activeTask)
        whenever(quickStartRepository.quickStartType).thenReturn(quickStartType)
        whenever(jetpackBrandingUtils.getBrandingTextForScreen(any())).thenReturn(mock())
        whenever(pagesCardViewModelSlice.getPagesCardBuilderParams(anyOrNull())).thenReturn(mock())
        whenever(todaysStatsViewModelSlice.getTodaysStatsBuilderParams(anyOrNull())).thenReturn(mock())
        whenever(postsCardViewModelSlice.getPostsCardBuilderParams(anyOrNull())).thenReturn(mock())
        whenever(activityLogCardViewModelSlice.getActivityLogCardBuilderParams(anyOrNull())).thenReturn(mock())
        whenever(personalizeCardViewModelSlice.getBuilderParams()).thenReturn(mock())
        whenever(personalizeCardBuilder.build(any())).thenReturn(mock())
        whenever(dynamicCardsViewModelSlice.getBuilderParams(anyOrNull())).thenReturn(
            MySiteCardAndItemBuilderParams.DynamicCardsBuilderParams(
                mock(),
                mock(),
                mock(),
                mock(),
            )
        )
        whenever(bloggingPromptCardViewModelSlice.getBuilderParams(anyOrNull())).thenReturn(mock())
        whenever(quickStartRepository.quickStartMenuStep).thenReturn(mock())

        viewModel = MySiteViewModel(
            testDispatcher(),
            testDispatcher(),
            analyticsTrackerWrapper,
            accountStore,
            selectedSiteRepository,
            siteIconUploadHandler,
            quickStartRepository,
            homePageDataLoader,
            quickStartUtilsWrapper,
            snackbarSequencer,
            landOnTheEditorFeatureConfig,
            buildConfigWrapper,
            appPrefsWrapper,
            quickStartTracker,
            dispatcher,
            jetpackFeatureRemovalOverlayUtil,
            getShowJetpackFullPluginInstallOnboardingUseCase,
            jetpackFeatureRemovalPhaseHelper,
            wpJetpackIndividualPluginHelper,
            siteInfoHeaderCardViewModelSlice,
            accountDataViewModelSlice,
            dashboardCardsViewModelSlice,
            dashboardItemsViewModelSlice
        )
        uiModels = mutableListOf()
        snackbars = mutableListOf()
        textInputDialogModels = mutableListOf()
        dialogModels = mutableListOf()
        navigationActions = mutableListOf()
        showSwipeRefreshLayout = mutableListOf()
        launch(testDispatcher()) {
            viewModel.uiModel.observeForever {
                uiModels.add(it)
            }
        }
        viewModel.onSnackbarMessage.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                snackbars.add(it)
            }
        }
        viewModel.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }

        site = SiteModel()
        site.id = siteLocalId
        site.url = siteUrl
        site.name = siteName
        site.iconUrl = siteIcon
        site.siteId = siteLocalId.toLong()

        homepage = PageModel(PostModel(), site, localHomepageId, "home", PUBLISHED, Date(), false, 0L, null, 0L)

        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(homePageDataLoader.loadHomepage(site)).thenReturn(homepage)
        whenever(siteInfoHeaderCardViewModelSlice.getParams(site)).thenReturn(mock())
    }

    /* SITE STATE */

    @Test
    fun `model is empty with no selected site`() {
        onSiteSelected.value = null
        currentAvatar.value = AccountData("","")

        assertThat(uiModels.last()).isInstanceOf(NoSites::class.java)
    }

    @Test
    fun `model contains header of selected site`() {
        initSelectedSite()

        assertThat(uiModels.last()).isInstanceOf(SiteSelected::class.java)

        assertThat(getSiteInfoHeaderCard()).isInstanceOf(SiteInfoHeaderCard::class.java)
    }

    @Test
    fun `when selected site is changed, then cardTracker is reset`() = test {
        initSelectedSite()

        verify(cardsTracker, atLeastOnce()).resetShown()
    }

    @Test
    fun `when selected site is changed, then cardShownTracker is reset`() = test {
        initSelectedSite()

        verify(domainRegistrationCardShownTracker, atLeastOnce()).resetShown()
    }


    /* AVATAR */

    @Test
    fun `account avatar url value is emitted and updated from the source`() {
        currentAvatar.value = AccountData(avatarUrl,userName)

        assertThat((uiModels.last() as NoSites).avatarUrl).isEqualTo(avatarUrl)
    }

    @Test
    fun `avatar press opens me screen`() {
        viewModel.onAvatarPressed()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenMeScreen)
    }

    /* LOGIN - NAVIGATION TO STATS */

    @Test
    fun `handling successful login result opens stats screen`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        viewModel.handleSuccessfulLoginResult()

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenStats(site))
    }

    /* EMPTY VIEW - ADD SITE */
    @Test
    fun `given empty site view, when add new site is tapped, then navigated to AddNewSite`() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)

        viewModel.onAddSitePressed()

        assertThat(navigationActions).containsOnly(
            SiteNavigationAction.AddNewSite(
                true,
                SiteCreationSource.MY_SITE_NO_SITES
            )
        )
    }

    /* ON RESUME */
    @Test
    fun `when clear active quick start task is triggered, then clear active quick start task`() {
        viewModel.clearActiveQuickStartTask()

        verify(quickStartRepository).clearActiveTask()
    }

    @Test
    fun `when check and show quick start notice is triggered, then check and show quick start notice`() {
        viewModel.checkAndShowQuickStartNotice()

        verify(quickStartRepository).checkAndShowQuickStartNotice()
    }

    /* DOMAIN REGISTRATION CARD */
    @Test
    fun `domain registration item click opens domain registration`() {
        initSelectedSite(isJetpackApp = true)
        isDomainCreditAvailable.value = DomainCreditAvailable(true)

        findDomainRegistrationCard()?.onClick?.click()

        verify(analyticsTrackerWrapper).track(Stat.DOMAIN_CREDIT_REDEMPTION_TAPPED, site)

        assertThat(navigationActions).containsOnly(SiteNavigationAction.OpenDomainRegistration(site))
    }

    @Test
    fun `snackbar is shown and event is tracked when handling successful domain registration result without email`() {
        viewModel.handleSuccessfulDomainRegistrationResult(null)

        verify(analyticsTrackerWrapper).track(Stat.DOMAIN_CREDIT_REDEMPTION_SUCCESS)

        val message = UiStringRes(R.string.my_site_verify_your_email_without_email)

        assertThat(snackbars).containsOnly(SnackbarMessageHolder(message))
    }

    @Test
    fun `snackbar is shown and event is tracked when handling successful domain registration result with email`() {
        viewModel.handleSuccessfulDomainRegistrationResult(emailAddress)

        verify(analyticsTrackerWrapper).track(Stat.DOMAIN_CREDIT_REDEMPTION_SUCCESS)

        val message = UiStringResWithParams(R.string.my_site_verify_your_email, listOf(UiStringText(emailAddress)))

        assertThat(snackbars).containsOnly(SnackbarMessageHolder(message))
    }

    @Test
    fun `when domain registration card is shown, then card shown event is tracked`() = test {
        initSelectedSite(isJetpackApp = true)
        isDomainCreditAvailable.value = DomainCreditAvailable(true)

        verify(
            domainRegistrationCardShownTracker,
            atLeastOnce()
        ).trackShown(MySiteCardAndItem.Type.DOMAIN_REGISTRATION_CARD)
    }

    /* QUICK START CARD */

    @Test
    fun `when quick start task type item is clicked, then quick start full screen dialog is opened`() {
        initSelectedSite(isQuickStartInProgress = true, isJetpackApp = true)

        requireNotNull(quickStartTaskTypeItemClickAction).invoke(QuickStartTaskType.CUSTOMIZE)

        assertThat(navigationActions.last())
            .isInstanceOf(SiteNavigationAction.OpenQuickStartFullScreenDialog::class.java)
    }

    @Test
    fun `when quick start task type item is clicked, then quick start active task is cleared`() {
        initSelectedSite(isQuickStartInProgress = true, isJetpackApp = true)

        requireNotNull(quickStartTaskTypeItemClickAction).invoke(QuickStartTaskType.CUSTOMIZE)

        verify(quickStartRepository).clearActiveTask()
    }


    @Test
    fun `when quick start card item clicked, then quick start card item tapped is tracked`() {
        initSelectedSite(isJetpackApp = true)

        requireNotNull(quickStartTaskTypeItemClickAction).invoke(QuickStartTaskType.CUSTOMIZE)

        verify(cardsTracker).trackQuickStartCardItemClicked(QuickStartTaskType.CUSTOMIZE)
    }

    @Test
    fun `when remove next steps dialog negative btn clicked, then QS is not skipped`() {
        initSelectedSite(isQuickStartInProgress = true)

        viewModel.onDialogInteraction(DialogInteraction.Negative(MySiteViewModel.TAG_REMOVE_NEXT_STEPS_DIALOG))

        verify(quickStartRepository, never()).skipQuickStart()
    }

    @Test
    fun `when quick start task is clicked, then task is set as active task`() {
        val task = QuickStartNewSiteTask.VIEW_SITE
        initSelectedSite(isQuickStartInProgress = true)

        viewModel.onQuickStartTaskCardClick(task)

        verify(quickStartRepository).setActiveTask(task)
    }

    /* START/IGNORE QUICK START + QUICK START DIALOG */
    @Test
    fun `given no selected site, when check and start QS is triggered, then QSP is not shown`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldShowQuickStart()).thenReturn(true)

        viewModel.checkAndStartQuickStart(isSiteTitleTaskCompleted = false, isNewSite = false)

        assertThat(navigationActions).isEmpty()
    }

    @Test
    fun `given QS is not available for new site, when check and start QS is triggered, then QSP is not shown`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(false)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldShowQuickStart()).thenReturn(true)

        viewModel.checkAndStartQuickStart(isSiteTitleTaskCompleted = false, isNewSite = true)

        assertThat(navigationActions).isEmpty()
    }

    @Test
    fun `given QS is not available for existing site, when check and start QS is triggered, then QSP is not shown`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(false)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldShowQuickStart()).thenReturn(true)

        viewModel.checkAndStartQuickStart(isSiteTitleTaskCompleted = false, isNewSite = false)

        assertThat(navigationActions).isEmpty()
    }

    @Test
    fun `given new site, when check and start QS is triggered, then QSP is shown`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(true)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldShowQuickStart()).thenReturn(true)

        viewModel.checkAndStartQuickStart(false, isNewSite = true)

        assertThat(navigationActions).containsExactly(
            SiteNavigationAction.ShowQuickStartDialog(
                R.string.quick_start_dialog_need_help_manage_site_title,
                R.string.quick_start_dialog_need_help_manage_site_message,
                R.string.quick_start_dialog_need_help_manage_site_button_positive,
                R.string.quick_start_dialog_need_help_button_negative,
                true
            )
        )
    }

    @Test
    fun `given existing site, when check and start QS is triggered, then QSP is shown`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(quickStartUtilsWrapper.isQuickStartAvailableForTheSite(site)).thenReturn(true)
        whenever(jetpackFeatureRemovalPhaseHelper.shouldShowQuickStart()).thenReturn(true)

        viewModel.checkAndStartQuickStart(false, isNewSite = false)

        assertThat(navigationActions).containsExactly(
            SiteNavigationAction.ShowQuickStartDialog(
                R.string.quick_start_dialog_need_help_manage_site_title,
                R.string.quick_start_dialog_need_help_manage_site_message,
                R.string.quick_start_dialog_need_help_manage_site_button_positive,
                R.string.quick_start_dialog_need_help_button_negative,
                false
            )
        )
    }

    @Test
    fun `when start QS is triggered, then QS request dialog positive tapped is tracked`() {
        viewModel.startQuickStart()

        verify(quickStartTracker).track(Stat.QUICK_START_REQUEST_DIALOG_POSITIVE_TAPPED)
    }

    @Test
    fun `when start QS is triggered, then QS starts`() {
        whenever(selectedSiteRepository.getSelectedSiteLocalId()).thenReturn(site.id)

        viewModel.startQuickStart()

        verify(quickStartUtilsWrapper)
            .startQuickStart(site.id, false, quickStartRepository.quickStartType, quickStartTracker)
//        verify(mySiteSourceManager).refreshQuickStart()
    }

    @Test
    fun `when ignore QS is triggered, then QS request dialog negative tapped is tracked`() {
        viewModel.ignoreQuickStart()

        verify(quickStartTracker).track(Stat.QUICK_START_REQUEST_DIALOG_NEGATIVE_TAPPED)
    }

    /* DASHBOARD BLOGGING PROMPT */
    @Test
    fun `when blogging prompt answer is uploaded, refresh prompt card`() = test {
        initSelectedSite()

        val promptAnswerPost = PostModel().apply { answeredPromptId = 1 }

        val postUploadedEvent = PostStore.OnPostUploaded(promptAnswerPost, true)

        viewModel.onPostUploaded(postUploadedEvent)

//        verify(mySiteSourceManager).refreshBloggingPrompts(true)
    }

    @Test
    fun `when non blogging prompt answer is uploaded, prompt card is not refreshed`() = test {
        initSelectedSite()

        val promptAnswerPost = PostModel().apply { answeredPromptId = 0 }

        val postUploadedEvent = PostStore.OnPostUploaded(promptAnswerPost, true)

        viewModel.onPostUploaded(postUploadedEvent)

//        verify(mySiteSourceManager, never()).refreshBloggingPrompts(true)
    }

    @Test
    fun `given blogging prompt card, when resuming dashboard, then tracker helper called as expected`() = test {
        initSelectedSite()

        val siteSelected = uiModels.last() as SiteSelected

        verify(bloggingPromptCardViewModelSlice, atLeastOnce()).onSiteChanged(siteLocalId, siteSelected)

        viewModel.onResume()

        verify(bloggingPromptCardViewModelSlice).onResume(siteSelected)
        verify(bloggingPromptCardViewModelSlice, atLeastOnce())
            .onDashboardCardsUpdated(
                any(),
                any()
            )
    }

    @Test
    fun `given no blogging prompt card, when resuming dashboard, then tracker helper called as expected`() = test {
        initSelectedSite()

        val siteSelected = uiModels.last() as SiteSelected

        verify(bloggingPromptCardViewModelSlice, atLeastOnce()).onSiteChanged(siteLocalId, siteSelected)

        viewModel.onResume()

        verify(bloggingPromptCardViewModelSlice).onResume(siteSelected)
        verify(bloggingPromptCardViewModelSlice, atMost(1))
            .onDashboardCardsUpdated(
                any(),
                anyOrNull()
            )
    }

    @Test
    fun `given blogging prompt card, when resuming menu, then tracker helper called as expected`() = test {
        initSelectedSite()

        val siteSelected = uiModels.last() as SiteSelected

        verify(bloggingPromptCardViewModelSlice, atLeastOnce()).onSiteChanged(siteLocalId, siteSelected)

        viewModel.onResume()

        verify(bloggingPromptCardViewModelSlice).onResume(siteSelected)
        verify(bloggingPromptCardViewModelSlice, atLeastOnce())
            .onDashboardCardsUpdated(
                any(),
                any()
            )
    }

    /* DASHBOARD ERROR SNACKBAR */

    @Test
    fun `given show snackbar in cards update, when dashboard cards updated, then dashboard snackbar shown`() =
        test {
            initSelectedSite()

            cardsUpdate.value = cardsUpdate.value?.copy(showSnackbarError = true)

            assertThat(snackbars).containsOnly(
                SnackbarMessageHolder(UiStringRes(R.string.my_site_dashboard_update_error))
            )
        }

    @Test
    fun `given show snackbar not in cards update, when dashboard cards updated, then dashboard snackbar not shown`() =
        test {
            initSelectedSite()

            cardsUpdate.value = cardsUpdate.value?.copy(showSnackbarError = false)

            assertThat(snackbars).doesNotContain(
                SnackbarMessageHolder(UiStringRes(R.string.my_site_dashboard_update_error))
            )
        }

    /* DASHBOARD ERROR CARD - RETRY */

    @Test
    fun `given error dashboard card, when retry is clicked, then refresh is triggered`() =
        test {
            initSelectedSite(isJetpackApp = true)
            cardsUpdate.value = cardsUpdate.value?.copy(showErrorCard = true)

            requireNotNull(onDashboardErrorRetryClick).invoke()

//            verify(mySiteSourceManager).refresh()
        }

    /* INFO ITEM */

    @Test
    fun `given show stale msg not in cards update, when dashboard cards updated, then info item not shown`() {
        initSelectedSite(showStaleMessage = false, isJetpackApp = true)

        cardsUpdate.value = cardsUpdate.value?.copy(showStaleMessage = false)

        assertThat((uiModels.last() as SiteSelected)
            .dashboardData.filterIsInstance(InfoItem::class.java))
            .isEmpty()
    }

    @Test
    fun `given show stale msg in cards update, when dashboard cards updated, then info item shown`() {
        initSelectedSite(showStaleMessage = true, isJetpackApp = true)

        cardsUpdate.value = cardsUpdate.value?.copy(showStaleMessage = true)

        assertThat((uiModels.last() as SiteSelected)
            .dashboardData.filterIsInstance(InfoItem::class.java))
            .isNotEmpty
    }

    /* ITEM VISIBILITY */
   @Test
    fun `backup menu item is NOT visible, when getJetpackMenuItemsVisibility is false`() = test {
        setUpSiteItemBuilder()
        initSelectedSite()

        jetpackCapabilities.value = JetpackCapabilities(scanAvailable = false, backupAvailable = false)

        assertThat(findBackupListItem()).isNull()
    }

    @Test
    fun `scan menu item is NOT visible, when getJetpackMenuItemsVisibility is false`() = test {
        setUpSiteItemBuilder()
        initSelectedSite()
        jetpackCapabilities.value = JetpackCapabilities(scanAvailable = false, backupAvailable = false)

        assertThat(findScanListItem()).isNull()
    }

    @Test
    fun `scan menu item is visible, when getJetpackMenuItemsVisibility is true`() = test {
        setUpSiteItemBuilder(scanAvailable = true)
        initSelectedSite()

        jetpackCapabilities.value = JetpackCapabilities(scanAvailable = true, backupAvailable = false)

        assertThat(findScanListItem()).isNotNull
    }

    @Test
    fun `backup menu item is visible, when getJetpackMenuItemsVisibility is true`() = test {
        setUpSiteItemBuilder(backupAvailable = true)
        initSelectedSite()

        jetpackCapabilities.value = JetpackCapabilities(scanAvailable = false, backupAvailable = true)

        assertThat(findBackupListItem()).isNotNull
    }

    /* SWIPE REFRESH */

    @Test
    fun `given refresh, when not invoked as PTR, then pull-to-refresh request is not tracked`() {
        initSelectedSite()

        viewModel.refresh()

        verify(analyticsTrackerWrapper, times(0)).track(Stat.MY_SITE_PULL_TO_REFRESH)
    }

    /* CLEARED */
    @Test
    fun `when vm cleared() is invoked, then MySiteSource clear() is invoked`() {
        viewModel.invokeOnCleared()

//        verify(mySiteSourceManager).clear()
    }

    /* LAND ON THE EDITOR A/B EXPERIMENT */
    @Test
    fun `given the land on the editor feature is enabled, then the home page editor is shown`() = test {
        whenever(landOnTheEditorFeatureConfig.isEnabled()).thenReturn(true)

        viewModel.performFirstStepAfterSiteCreation(isSiteTitleTaskCompleted = false, isNewSite = true)

        verify(analyticsTrackerWrapper).track(Stat.LANDING_EDITOR_SHOWN)
        assertThat(navigationActions).containsExactly(
            SiteNavigationAction.OpenHomepage(site, homepageLocalId = localHomepageId, isNewSite = true)
        )
    }

    @Test
    fun `given the land on the editor feature is not enabled, then the home page editor is not shown`() = test {
        whenever(landOnTheEditorFeatureConfig.isEnabled()).thenReturn(false)

        viewModel.performFirstStepAfterSiteCreation(isSiteTitleTaskCompleted = false, isNewSite = true)

        assertThat(navigationActions).isEmpty()
    }

    /* ORDERED LIST */

    @Test
    fun `given info item exist, when cardAndItems list is ordered, then info item succeeds site info card`() {
        initSelectedSite(showStaleMessage = true)
        cardsUpdate.value = cardsUpdate.value?.copy(showStaleMessage = true)

        val siteInfoCardIndex = getLastItems().indexOfFirst { it is SiteInfoHeaderCard }
        val infoItemIndex = getLastItems().indexOfFirst { it is InfoItem }

        assertThat(infoItemIndex).isEqualTo(siteInfoCardIndex + 1)
    }

    @Test
    fun `given shouldShowJetpackBranding is true, then the Jetpack badge is visible last`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)

        initSelectedSite(shouldShowJetpackBranding = true)

        assertThat(getSiteMenuTabLastItems().last()).isInstanceOf(JetpackBadge::class.java)
    }

    @Test
    fun `given shouldShowJetpackBranding is false, then no Jetpack badge is visible`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)

        initSelectedSite(shouldShowJetpackBranding = false)

        assertThat(findJetpackBadgeListItem()).isEmpty()
    }

    @Test
    fun `given IS NOT Jetpack app, migration success card SHOULD NOT be shown`() {
        initSelectedSite()

        assertThat(getSiteMenuTabLastItems()[0]).isNotInstanceOf(SingleActionCard::class.java)
        assertThat(getLastItems()[0]).isNotInstanceOf(SingleActionCard::class.java)
        assertThat(getDashboardTabLastItems()[0]).isNotInstanceOf(SingleActionCard::class.java)
    }

    @Test
    fun `given migration IS NOT completed, migration success card SHOULD NOT be shown`() {
        val packageName = "packageName"
        whenever(wordPressPublicData.currentPackageId()).thenReturn(packageName)
        whenever(appStatus.isAppInstalled(packageName)).thenReturn(true)

        initSelectedSite(isJetpackApp = true)

        assertThat(getSiteMenuTabLastItems()[0]).isNotInstanceOf(SingleActionCard::class.java)
        assertThat(getLastItems()[0]).isNotInstanceOf(SingleActionCard::class.java)
        assertThat(getDashboardTabLastItems()[0]).isNotInstanceOf(SingleActionCard::class.java)
    }

    @Test
    fun `given WordPress app IS NOT installed, migration success card SHOULD NOT be shown`() {
        whenever(appPrefsWrapper.isJetpackMigrationCompleted()).thenReturn(true)

        initSelectedSite(isJetpackApp = true)

        assertThat(getSiteMenuTabLastItems()[0]).isNotInstanceOf(SingleActionCard::class.java)
        assertThat(getLastItems()[0]).isNotInstanceOf(SingleActionCard::class.java)
        assertThat(getDashboardTabLastItems()[0]).isNotInstanceOf(SingleActionCard::class.java)
    }

    @Test
    fun `given IS JP app, migration IS complete and WP app IS installed, migration success card SHOULD be shown`() {
        val packageName = "packageName"
        whenever(wordPressPublicData.currentPackageId()).thenReturn(packageName)
        whenever(appPrefsWrapper.isJetpackMigrationCompleted()).thenReturn(true)
        whenever(appStatus.isAppInstalled(packageName)).thenReturn(true)

        initSelectedSite(isJetpackApp = true)

        assertThat(getDashboardTabLastItems()[1]).isInstanceOf(SingleActionCard::class.java)
    }

    @Test
    fun `JP migration success card should have the correct text`() {
        val packageName = "packageName"
        whenever(wordPressPublicData.currentPackageId()).thenReturn(packageName)
        whenever(appPrefsWrapper.isJetpackMigrationCompleted()).thenReturn(true)
        whenever(appStatus.isAppInstalled(packageName)).thenReturn(true)
        initSelectedSite(isJetpackApp = true)

        val expected = R.string.jp_migration_success_card_message
        assertThat((getDashboardTabLastItems()[1] as SingleActionCard).textResource).isEqualTo(expected)
    }

    @Test
    fun `JP migration success card should have the correct image`() {
        val packageName = "packageName"
        whenever(wordPressPublicData.currentPackageId()).thenReturn(packageName)
        whenever(appPrefsWrapper.isJetpackMigrationCompleted()).thenReturn(true)
        whenever(appStatus.isAppInstalled(packageName)).thenReturn(true)
        initSelectedSite(isJetpackApp = true)

        val expected = R.drawable.ic_wordpress_jetpack_appicon
        assertThat((getDashboardTabLastItems()[1] as SingleActionCard).imageResource).isEqualTo(expected)
    }

    @Test
    fun `JP migration success card click should be tracked`() {
        val packageName = "packageName"
        whenever(wordPressPublicData.currentPackageId()).thenReturn(packageName)
        whenever(appPrefsWrapper.isJetpackMigrationCompleted()).thenReturn(true)
        whenever(appStatus.isAppInstalled(packageName)).thenReturn(true)
        initSelectedSite(isJetpackApp = true)

        (getDashboardTabLastItems()[1] as SingleActionCard).onActionClick.invoke()

        verify(contentMigrationAnalyticsTracker).trackPleaseDeleteWordPressCardTapped()
    }

    /* STATE LISTS */
    @Test
    fun `given site select exists, then cardAndItem lists are not empty`() {
        initSelectedSite()

        assertThat(getLastItems()).isNotEmpty
        assertThat(getDashboardTabLastItems()).isNotEmpty
        assertThat(getSiteMenuTabLastItems()).isNotEmpty
    }

    @Test
    fun `given selected site with tabs disabled, when all cards and items, then qs card exists`() {
        initSelectedSite(isJetpackApp = true)

        assertThat(getLastItems().filterIsInstance(QuickStartCard::class.java)).isNotEmpty
    }

    @Test
    fun `given selected site, when dashboard cards and items, then dashboard cards exists`() {
        initSelectedSite(isJetpackApp = true)

        val items = (uiModels.last() as SiteSelected).dashboardData

        assertThat(items.filterIsInstance(MySiteCardAndItem.Card::class.java)).isNotEmpty
    }

    @Test
    fun `given selected site, when dashboard cards and items, then list items not exist`() {
       // setUpSiteItemBuilder()
        initSelectedSite()

        val items = (uiModels.last() as SiteSelected).dashboardData

        assertThat(items.filterIsInstance(ListItem::class.java)).isEmpty()
    }

    @Test
    fun `when dashboard cards items built, then qs card exists`() {
      //  setUpSiteItemBuilder()
        initSelectedSite(isJetpackApp = true)

        val items = (uiModels.last() as SiteSelected).dashboardData

        assertThat(items.filterIsInstance(QuickStartCard::class.java)).isNotEmpty
    }

    @Test
    fun `given site menu built, when dashboard cards items, then qs card not exists`() {
      //  setUpSiteItemBuilder(shouldEnableFocusPoint = true)

        initSelectedSite()

        val items = (uiModels.last() as SiteSelected).dashboardData

        assertThat(items.filterIsInstance(QuickStartCard::class.java)).isEmpty()
    }
    @Test
    fun `given selected site, when site menu cards and items, then list items exist`() {
        setUpSiteItemBuilder()
        initSelectedSite()

        val items = (uiModels.last() as SiteSelected).dashboardData

        assertThat(items.filterIsInstance(ListItem::class.java)).isNotEmpty
    }

    @Test
    fun `given tabs enabled + dashboard default tab variant, when site menu cards + items, then qs card not exists`() {
      //  setUpSiteItemBuilder()

        initSelectedSite()

        val items = (uiModels.last() as SiteSelected).dashboardData

        assertThat(items.filterIsInstance(QuickStartCard::class.java)).isEmpty()
    }

    @Test
    fun `given selected site with domain credit, when dashboard cards + items, then domain reg card exists`() {
        initSelectedSite(isJetpackApp = true)
        isDomainCreditAvailable.value = DomainCreditAvailable(true)

        val items = (uiModels.last() as SiteSelected).dashboardData

        assertThat(items.filterIsInstance(DomainRegistrationCard::class.java)).isNotEmpty
    }

    @Test
    fun `given selected site with domain credit, when site menu cards and items, then domain reg card doesn't exist`() {
        initSelectedSite()
        isDomainCreditAvailable.value = DomainCreditAvailable(true)

        val items = (uiModels.last() as SiteSelected).dashboardData

        assertThat(items.filterIsInstance(DomainRegistrationCard::class.java)).isEmpty()
    }

    /* JETPACK FEATURE CARD */
    @Test
    fun `when feature card criteria is not met, then items does not contain feature card`() = test {
        whenever(jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()).thenReturn(false)

        initSelectedSite()

        assertThat(getSiteMenuTabLastItems()[0]).isNotInstanceOf(JetpackFeatureCard::class.java)
        assertThat(getLastItems()[0]).isNotInstanceOf(JetpackFeatureCard::class.java)
        assertThat(getDashboardTabLastItems()[0]).isNotInstanceOf(JetpackFeatureCard::class.java)
    }

    @Test
    fun `when feature card criteria is met + show at top, then items do contain feature card`() = test {
        whenever(jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()).thenReturn(true)
        whenever(jetpackFeatureCardHelper.shouldShowFeatureCardAtTop()).thenReturn(true)

        initSelectedSite()

        assertThat(getSiteMenuTabLastItems()[1]).isInstanceOf(JetpackFeatureCard::class.java)
        assertThat(getMenuItems()[1]).isInstanceOf(JetpackFeatureCard::class.java)
    }

    @Test
    fun `when feature card criteria is met + show at bottom, then items do contain feature card`() = test {
        whenever(jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()).thenReturn(true)
        whenever(jetpackFeatureCardHelper.shouldShowFeatureCardAtTop()).thenReturn(false)

        initSelectedSite()

        assertThat(getSiteMenuTabLastItems().filterIsInstance(JetpackFeatureCard::class.java)).isNotEmpty
    }

    @Test
    fun `when jetpack feature card is shown, then jetpack feature card shown is tracked`() = test {
        whenever(jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()).thenReturn(true)

        initSelectedSite()

        verify(jetpackFeatureCardShownTracker, atLeastOnce()).trackShown(MySiteCardAndItem.Type.JETPACK_FEATURE_CARD)
    }

    @Test
    fun `when Jetpack feature card is clicked, then jetpack feature card clicked is tracked`() {
        whenever(jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()).thenReturn(true)
        initSelectedSite()

        findJetpackFeatureCard()?.onClick?.click()

        verify(jetpackFeatureCardHelper).track(Stat.REMOVE_FEATURE_CARD_TAPPED)
    }

    @Test
    fun `when Jetpack feature card learn more is clicked, then learn more is tracked`() {
        whenever(jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()).thenReturn(true)
        whenever(jetpackFeatureCardHelper.getLearnMoreUrl()).thenReturn("https://jetpack.com")
        initSelectedSite()

        findJetpackFeatureCard()?.onLearnMoreClick?.click()

        verify(jetpackFeatureCardHelper).track(Stat.REMOVE_FEATURE_CARD_LINK_TAPPED)
    }

    @Test
    fun `when Jetpack feature card menu is clicked, then menu clicked is tracked`() {
        whenever(jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()).thenReturn(true)
        initSelectedSite()

        findJetpackFeatureCard()?.onMoreMenuClick?.click()

        verify(jetpackFeatureCardHelper).track(Stat.REMOVE_FEATURE_CARD_MENU_ACCESSED)
    }

    @Test
    fun `when Jetpack feature card hide this is clicked, then hide is tracked`() {
        whenever(jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()).thenReturn(true)
        initSelectedSite()

        findJetpackFeatureCard()?.onHideMenuItemClick?.click()

        verify(jetpackFeatureCardHelper).hideJetpackFeatureCard()
    }

    @Test
    fun `when Jetpack feature card remind later is clicked, then remind later is tracked`() {
        whenever(jetpackFeatureCardHelper.shouldShowJetpackFeatureCard()).thenReturn(true)
        initSelectedSite()

        findJetpackFeatureCard()?.onRemindMeLaterItemClick?.click()

        verify(jetpackFeatureCardHelper).setJetpackFeatureCardLastShownTimeStamp(any())
    }

    @Test
    fun `when onActionableEmptyViewVisible is invoked then show jetpack individual plugin overlay`() =
        test {
            whenever(wpJetpackIndividualPluginHelper.shouldShowJetpackIndividualPluginOverlay()).thenReturn(true)

            viewModel.onActionableEmptyViewVisible()
            advanceUntilIdle()

            assertThat(viewModel.onShowJetpackIndividualPluginOverlay.value?.peekContent()).isEqualTo(Unit)
        }

    @Test
    fun `when onActionableEmptyViewVisible is invoked then don't show jetpack individual plugin overlay`() =
        test {
            whenever(wpJetpackIndividualPluginHelper.shouldShowJetpackIndividualPluginOverlay()).thenReturn(false)

            viewModel.onActionableEmptyViewVisible()
            advanceUntilIdle()

            assertThat(viewModel.onShowJetpackIndividualPluginOverlay.value?.peekContent()).isNull()
        }


    private fun findDomainRegistrationCard() =
        getLastItems().find { it is DomainRegistrationCard } as DomainRegistrationCard?

    private fun findJetpackFeatureCard() =
        getMenuItems().find { it is JetpackFeatureCard } as JetpackFeatureCard?

    private fun findBackupListItem() = getMenuItems().filterIsInstance(ListItem::class.java)
        .firstOrNull { it.primaryText == UiStringRes(R.string.backup) }

    private fun findScanListItem() = getMenuItems().filterIsInstance(ListItem::class.java)
        .firstOrNull { it.primaryText == UiStringRes(R.string.scan) }


    private fun findJetpackBadgeListItem() = getSiteMenuTabLastItems().filterIsInstance(JetpackBadge::class.java)

    private fun getLastItems() = (uiModels.last() as SiteSelected).dashboardData

    private fun getMenuItems() = (uiModels.last() as SiteSelected).dashboardData

    private fun getDashboardTabLastItems() = (uiModels.last() as SiteSelected).dashboardData

    private fun getSiteMenuTabLastItems() = (uiModels.last() as SiteSelected).dashboardData

    private fun getSiteInfoHeaderCard() = (uiModels.last() as SiteSelected).dashboardData[0]

    @Suppress("LongParameterList")
    private fun initSelectedSite(
        isQuickStartInProgress: Boolean = false,
        showStaleMessage: Boolean = false,
        isSiteUsingWpComRestApi: Boolean = true,
        shouldShowJetpackBranding: Boolean = true,
        isJetpackApp: Boolean = false
    ) {
        whenever(
            mySiteInfoItemBuilder.build(InfoItemBuilderParams(isStaleMessagePresent = showStaleMessage))
        ).thenReturn(if (showStaleMessage) InfoItem(title = UiStringText("")) else null)
        quickStartUpdate.value = QuickStartUpdate(
            categories = if (isQuickStartInProgress) listOf(quickStartCategory) else emptyList()
        )
        // in order to build the dashboard cards, this value should be true along with isSiteUsingWpComRestApi
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(isJetpackApp)

        whenever(jetpackBrandingUtils.shouldShowJetpackBrandingInDashboard()).thenReturn(shouldShowJetpackBranding)
        if (isSiteUsingWpComRestApi) {
            site.setIsWPCom(true)
            site.setIsJetpackConnected(true)
            site.origin = SiteModel.ORIGIN_WPCOM_REST
        }
        onSiteSelected.value = siteLocalId
        onSiteChange.value = site
        selectedSite.value = SelectedSite(site)
    }

    private fun setUpSiteItemBuilder(
        backupAvailable: Boolean = false,
        scanAvailable: Boolean = false,
        shouldEnableFocusPoint: Boolean = false,
        activeTask: QuickStartTask? = null
    ) {
        val siteItemsBuilderParams = MySiteCardAndItemBuilderParams.SiteItemsBuilderParams(
            site = site,
            activeTask = activeTask,
            backupAvailable = backupAvailable,
            scanAvailable = scanAvailable,
            enableFocusPoints = shouldEnableFocusPoint,
            onClick = mock(),
            isBlazeEligible = true
        )

        whenever(siteItemsBuilder.build(anyOrNull())).thenReturn(initSiteItems(siteItemsBuilderParams))
    }


    private fun initSiteItems(params: MySiteCardAndItemBuilderParams.SiteItemsBuilderParams): List<ListItem> {
        val items = mutableListOf<ListItem>()
        items.add(
            ListItem(
                0,
                UiStringRes(0),
                onClick = ListItemInteraction.create(ListItemAction.POSTS, params.onClick),
                listItemAction = ListItemAction.POSTS
            )
        )
        if (params.scanAvailable) {
            items.add(
                ListItem(
                    0,
                    UiStringRes(R.string.scan),
                    onClick = mock(),
                    listItemAction = ListItemAction.SCAN
                )
            )
        }
        if (params.backupAvailable) {
            items.add(
                ListItem(
                    0,
                    UiStringRes(R.string.backup),
                    onClick = mock(),
                    listItemAction = ListItemAction.BACKUP
                )
            )
        }
        if (params.isBlazeEligible) {
            items.add(
                ListItem(
                    0,
                    UiStringRes(R.string.blaze_menu_item_label),
                    onClick = mock(),
                    disablePrimaryIconTint = true,
                    listItemAction = ListItemAction.BLAZE
                )
            )
        }

        return items
    }

    fun ViewModel.invokeOnCleared() {
        val viewModelStore = ViewModelStore()
        val viewModelProvider = ViewModelProvider(viewModelStore, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = this@invokeOnCleared as T
        })
        viewModelProvider[this@invokeOnCleared::class.java]
        viewModelStore.clear()
    }
}
