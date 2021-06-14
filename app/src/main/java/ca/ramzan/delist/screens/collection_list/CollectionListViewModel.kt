package ca.ramzan.delist.screens.collection_list

import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.ramzan.delist.room.CollectionDatabaseDao
import ca.ramzan.delist.room.CollectionDisplayData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionListViewModel @Inject constructor(
    private val dao: CollectionDatabaseDao,
    private val prefs: SharedPreferences
) :
    ViewModel() {

    private var job: Job? = null

    private val _state = MutableStateFlow<ListState>(ListState.Loading)
    val state: StateFlow<ListState> get() = _state

    init {
        getCollections()
    }

    fun getCollections() {
        job?.cancel()
        job = viewModelScope.launch {
            when (val order =
                prefs.getString(PREF_COLLECTION_ORDER_KEY, PREF_COLLECTION_ORDER_MANUAL)) {
                PREF_COLLECTION_ORDER_MANUAL -> {
                    dao.getCollectionDisplaysManual()
                }
                PREF_COLLECTION_ORDER_ASC -> {
                    dao.getCollectionDisplaysAsc()
                }
                PREF_COLLECTION_ORDER_DESC -> {
                    dao.getCollectionDisplaysDesc()
                }
                else -> throw Exception("Illegal order: $order")
            }.collect { list ->
                val filteredList = if (prefs.getBoolean(PREF_COLLECTION_HIDE_ARCHIVED, true)) {
                    list.filter { !it.archived }
                } else list

                _state.emit(
                    if (filteredList.isEmpty()) ListState.NoCollections else ListState.Loaded(
                        filteredList
                    )
                )

            }
        }
    }

    fun completeTask(collectionId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            dao.completeTask(collectionId)
        }
    }

    fun undoCompleteTask(collectionId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            dao.undoCompleteTask(collectionId)
        }
    }

    fun moveItem(fromPos: Int, toPos: Int) {
        (state.value as? ListState.Loaded)?.run {
            CoroutineScope(Dispatchers.IO).launch {
                dao.updateCollectionsOrder(
                    collections[fromPos].id,
                    collections[toPos].id
                )
            }
        }
    }
}

sealed class ListState {
    object Loading : ListState()
    object NoCollections : ListState()
    data class Loaded(val collections: List<CollectionDisplayData>) : ListState()
}