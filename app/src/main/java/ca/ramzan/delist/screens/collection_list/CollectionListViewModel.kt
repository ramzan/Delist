package ca.ramzan.delist.screens.collection_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.ramzan.delist.room.CollectionDatabaseDao
import ca.ramzan.delist.room.CollectionDisplayData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionListViewModel @Inject constructor(private val dao: CollectionDatabaseDao) :
    ViewModel() {

    val collections = MutableStateFlow<List<CollectionDisplayData>>(emptyList())

    init {
        viewModelScope.launch {
            dao.getCollectionDisplaysManual().collect {
                collections.emit(it)
            }
        }
    }

    fun completeTask(collectionId: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            dao.completeTask(collectionId)
        }
    }

    fun moveItem(fromPos: Int, toPos: Int) {
        collections.value.run {
            val updatedList = this.map { it.id }.toMutableList()
            updatedList.add(toPos, updatedList.removeAt(fromPos))
            CoroutineScope(Dispatchers.IO).launch {
                dao.updateCollectionsOrder(updatedList)
            }
        }
    }
}