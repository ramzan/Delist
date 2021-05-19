package ca.ramzan.delist.screens.collection_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.ramzan.delist.room.CollectionDatabaseDao
import ca.ramzan.delist.room.CollectionDisplayData
import ca.ramzan.delist.room.CompletedTaskDisplay
import ca.ramzan.delist.room.Task
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch


class CollectionDetailViewModel @AssistedInject constructor(
    @Assisted private val collectionId: Long,
    private val dao: CollectionDatabaseDao
) : ViewModel() {

    val state = MutableStateFlow<DetailState>(DetailState.Loading)

    init {
        viewModelScope.launch {
            dao.getCollectionDisplay(collectionId)
                .combine(dao.getCompletedTasks(collectionId)) { collectionData, completedTasks ->
                    if (collectionData == null) DetailState.Deleted
                    else DetailState.Loaded(collectionData, completedTasks)
                }.collect {
                    state.emit(it)
                }
        }
    }

    fun deleteCollection() {
        CoroutineScope(Dispatchers.IO).launch {
            (state.value as? DetailState.Loaded)?.run {
                dao.deleteCollection(dao.getCollection(collectionId))
            }
        }
    }

    fun addTasks(tasks: String) {
        CoroutineScope(Dispatchers.IO).launch {
            dao.addTasks(tasks.split("\n")
                .map { it.trim() }
                .filter { it.isNotEmpty() }
                .map { Task(collectionId, it) }
            )
        }
    }

    fun completeTask() {
        CoroutineScope(Dispatchers.IO).launch {
            dao.completeTask(collectionId)
        }
    }

    fun clearCompleted() {
        CoroutineScope(Dispatchers.IO).launch {
            dao.deleteCompletedTasks(collectionId)
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
        val collection: CollectionDisplayData,
        val completedTasks: List<CompletedTaskDisplay>
    ) : DetailState()
}