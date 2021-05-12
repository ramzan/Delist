package ca.ramzan.delist.screens.collection_editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.ramzan.delist.room.Collection
import ca.ramzan.delist.room.CollectionColor
import ca.ramzan.delist.room.CollectionDatabaseDao
import ca.ramzan.delist.room.CollectionType
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch


class CollectionEditorViewModel @AssistedInject constructor(
    @Assisted private val collectionId: Long,
    private val dao: CollectionDatabaseDao
) : ViewModel() {

    val state = MutableStateFlow<EditorState>(EditorState.Loading)

    init {
        viewModelScope.launch {
            if (collectionId == 0L) state.emit(EditorState.Loaded())
            else {
                CoroutineScope(Dispatchers.IO).launch {
                    val oldCollection = dao.getCollection(collectionId)
                    state.emit(
                        EditorState.Loaded(
                            oldCollection.name,
                            oldCollection.type,
                            oldCollection.color,
                            oldCollection
                        )
                    )
                }
            }
        }
    }

    fun saveCollection() {
        CoroutineScope(Dispatchers.IO).launch {
            (state.value as? EditorState.Loaded)?.run {
                if (collectionId == 0L) dao.createCollection(
                    Collection(collectionType, nameInputText, color, null)
                ) else {
                    val collection = oldCollection!!
                    dao.updateCollection(
                        collection.copy(type = collectionType, name = nameInputText, color = color),
                        collection.type != collectionType
                    )
                }
            }
        }
    }

    fun updateColor(newColor: CollectionColor) {
        viewModelScope.launch {
            (state.value as? EditorState.Loaded)?.run {
                state.emit(this.copy(color = newColor))
            }
        }
    }

    // region Factory ------------------------------------------------------------------------------

    @AssistedFactory
    interface Factory {
        fun create(collectionId: Long): CollectionEditorViewModel
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

sealed class EditorState {

    object Loading : EditorState()
    data class Loaded(
        var nameInputText: String = "",
        var collectionType: CollectionType = CollectionType.QUEUE,
        val color: CollectionColor = CollectionColor.ORANGE,
        val oldCollection: Collection? = null
    ) : EditorState()
}