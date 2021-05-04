package ca.ramzan.delist.screens.collection_editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import ca.ramzan.delist.room.Collection
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
            state.emit(
                if (collectionId == 0L) EditorState.Loaded("", CollectionType.QUEUE, null)
                else {
                    val oldCollection = dao.getCollection(collectionId)
                    EditorState.Loaded(oldCollection.name, oldCollection.type, oldCollection)
                }
            )
        }
    }

    fun saveCollection() {
        CoroutineScope(Dispatchers.IO).launch {
            (state.value as? EditorState.Loaded)?.run {
                if (collectionId == 0L) dao.createCollection(
                    Collection(
                        collectionType,
                        nameInputText,
                        "880000",
                        null
                    )
                )
                else dao.updateCollection(
                    oldCollection!!.copy(
                        type = collectionType,
                        name = nameInputText
                    )
                )
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
    class Loaded(
        var nameInputText: String,
        var collectionType: CollectionType,
        val oldCollection: Collection?
    ) : EditorState()
}