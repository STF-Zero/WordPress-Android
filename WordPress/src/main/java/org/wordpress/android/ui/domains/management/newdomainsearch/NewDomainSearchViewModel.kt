package org.wordpress.android.ui.domains.management.newdomainsearch

import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.flow.debounce
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.domains.management.newdomainsearch.domainsfetcher.NewDomain
import org.wordpress.android.ui.domains.management.newdomainsearch.domainsfetcher.NewDomainsSearchRepository
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

@OptIn(FlowPreview::class)
@HiltViewModel
class NewDomainSearchViewModel @Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    private val newDomainsSearchRepository: NewDomainsSearchRepository,
    analyticsTracker: AnalyticsTrackerWrapper
) : ScopedViewModel(mainDispatcher) {
    private val _actionEvents = MutableSharedFlow<ActionEvent>()
    val actionEvents: Flow<ActionEvent> = _actionEvents

    private val _uiStateFlow: MutableStateFlow<UiState> = MutableStateFlow(UiState.Empty)
    val uiStateFlow = _uiStateFlow.asStateFlow()

    private val debouncedQuery = Channel<String>()

    init {
        analyticsTracker.track(AnalyticsTracker.Stat.DOMAIN_MANAGEMENT_SEARCH_FOR_A_DOMAIN_SCREEN_SHOWN)
        launch {
            debouncedQuery.consumeAsFlow()
                .debounce(300)
                .collect {
                    fetchDomains(it)
                }
        }
    }

    fun onSearchQueryChanged(query: String) {
        launch { debouncedQuery.send(query) }
    }

    private suspend fun fetchDomains(query: String) {
        val result = newDomainsSearchRepository.searchForDomains(query)
        _uiStateFlow.emit(
            when (result) {
                is NewDomainsSearchRepository.DomainsResult.Success -> UiState.PopulatedDomains(result.suggestions)
                is NewDomainsSearchRepository.DomainsResult.Error -> UiState.Error
                is NewDomainsSearchRepository.DomainsResult.Empty -> UiState.Empty
            }
        )
    }

    fun onBackPressed() {
        launch {
            _actionEvents.emit(ActionEvent.GoBack)
        }
    }

    sealed class ActionEvent {
        object GoBack : ActionEvent()
    }

    sealed class UiState {
        object Empty : UiState()
        object Error : UiState()
        data class PopulatedDomains(val domains: List<NewDomain>) : UiState()
    }
}
