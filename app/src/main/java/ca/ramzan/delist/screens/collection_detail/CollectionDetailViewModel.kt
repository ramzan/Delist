package ca.ramzan.delist.screens.collection_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.ramzan.delist.room.Collection
import ca.ramzan.delist.room.CollectionDatabaseDao
import ca.ramzan.delist.room.CompletedItemDisplay
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


class CollectionDetailViewModel @AssistedInject constructor(
    @Assisted private val collectionId: Long,
    private val dao: CollectionDatabaseDao
) : ViewModel() {

    val state = MutableStateFlow<DetailState>(DetailState.Loading)

    init {
        viewModelScope.launch {
            CoroutineScope(Dispatchers.IO).launch {
                state.emit(
                    DetailState.Loaded(
                        dao.getCollection(collectionId),
                        dao.getCompleteItems(collectionId)
                    )
                )
            }
        }
    }

    fun deleteCollection() {
        CoroutineScope(Dispatchers.IO).launch {
            (state.value as? DetailState.Loaded)?.run {
                dao.deleteCollection(collection)
                state.emit(DetailState.Deleted)
            }
        }
    }

    // region Factory ------------------------------------------------------------------------------

    @AssistedFactory
    interface Factory {
        fun create(collectionId: Long): CollectionDetailViewModel
    }

    companion object {
        fun provideFactory(
            assistedFactory: Factory,
            collectionId: Long
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return assistedFactory.create(collectionId) as T
            }
        }
    }

// endregion Factory ---------------------------------------------------------------------------
}

sealed class DetailState {

    object Loading : DetailState()
    object Deleted : DetailState()
    data class Loaded(
        val collection: Collection,
        val completedItems: List<CompletedItemDisplay>
    ) : DetailState()
}