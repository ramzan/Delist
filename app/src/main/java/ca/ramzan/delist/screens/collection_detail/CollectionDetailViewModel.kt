package ca.ramzan.delist.screens.collection_detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.ramzan.delist.room.CollectionDatabaseDao
import ca.ramzan.delist.room.CollectionDisplayData
import ca.ramzan.delist.room.Task
import ca.ramzan.delist.room.TaskDisplay
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted.Companion.Eagerly
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed class DetailState {

    object Loading : DetailState()
    object Deleted : DetailState()
    data class Loaded(
        val collection: CollectionDisplayData,
        val completedTasks: List<TaskDisplay>,
        val incompleteTasks: List<TaskDisplay>,
        val showCompleted: Boolean = true,
        val showIncomplete: Boolean = true
    ) : DetailState()
}

private data class ListVisibility(
    val showCompleted: Boolean = true,
    val showIncomplete: Boolean = false
) {
    fun toggleCompleted() = this.copy(showCompleted = !showCompleted)
    fun toggleIncomplete() = this.copy(showIncomplete = !showIncomplete)
}

class CollectionDetailViewModel @AssistedInject constructor(
    @Assisted private val collectionId: Long,
    private val dao: CollectionDatabaseDao
) : ViewModel() {

    private val listVisibility = MutableStateFlow(ListVisibility())
    fun toggleCompletedShown() {
        viewModelScope.launch {
            listVisibility.value = listVisibility.value.toggleCompleted()
        }
    }

    fun toggleIncompleteShown() {
        viewModelScope.launch {
            listVisibility.value = listVisibility.value.toggleIncomplete()
        }
    }

    private val tasks = dao.getTasks(collectionId).map { tasks ->
        val completeStart = tasks.indexOfFirst { it.completed != null }.let {
            if (it == -1) tasks.size else it
        }
        Pair(
            tasks.subList(completeStart, tasks.size),
            tasks.subList(0, completeStart)
        )
    }.stateIn(CoroutineScope(Dispatchers.IO), Eagerly, Pair(emptyList(), emptyList()))

    val state = dao.getCollectionDisplay(collectionId)
        .combine(tasks) { collectionData, tasks ->
            if (collectionData == null) DetailState.Deleted
            else DetailState.Loaded(collectionData, tasks.first, tasks.second)
        }.combine(listVisibility) { detailState, visibility ->
            if (detailState is DetailState.Loaded) detailState.copy(
                showCompleted = visibility.showCompleted,
                showIncomplete = visibility.showIncomplete
            ) else detailState
        }.stateIn(CoroutineScope(Dispatchers.Default), Eagerly, DetailState.Loading)

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


    fun undoCompleteTask() {
        CoroutineScope(Dispatchers.IO).launch {
            dao.undoCompleteTask(collectionId)
        }
    }

    fun clearCompleted() {
        CoroutineScope(Dispatchers.IO).launch {
            dao.deleteCompletedTasks(collectionId)
        }
    }

    fun archiveCollection(archived: Boolean) {
        CoroutineScope(Dispatchers.IO).launch {
            dao.archiveCollection(collectionId, archived)
        }
    }

    suspend fun exportList(): List<Task> {
        return dao.getAllTasks(collectionId)
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