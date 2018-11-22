package org.wordpress.android.ui.stats.refresh.lists.sections.days

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import kotlinx.coroutines.experimental.CoroutineDispatcher
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.stats.refresh.lists.Empty
import org.wordpress.android.ui.stats.refresh.lists.StatsUiState
import org.wordpress.android.ui.stats.refresh.lists.StatsUiState.StatsListState.DONE
import org.wordpress.android.ui.stats.refresh.lists.NavigationTarget
import org.wordpress.android.ui.stats.refresh.lists.StatsListViewModel
import javax.inject.Inject
import javax.inject.Named

class DaysListViewModel @Inject constructor(
    @Named(UI_THREAD) mainDispatcher: CoroutineDispatcher
) : StatsListViewModel(mainDispatcher) {
    private val _data = MutableLiveData<StatsUiState>()
    override val data: LiveData<StatsUiState> = _data

    override val navigationTarget: LiveData<NavigationTarget> = MutableLiveData()

    init {
        _data.value = StatsUiState(
                listOf(Empty()),
                DONE
        )
    }
}
