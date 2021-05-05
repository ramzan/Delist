package ca.ramzan.delist.screens.collection_list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ca.ramzan.delist.room.CollectionDatabaseDao
import ca.ramzan.delist.room.CollectionDisplayData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CollectionListViewModel @Inject constructor(dao: CollectionDatabaseDao) : ViewModel() {

    val collections = MutableStateFlow<List<CollectionDisplayData>>(emptyList())

    init {
        viewModelScope.launch {
            dao.getCollectionDisplays().collect {
                collections.emit(it)
            }
        }
    }
}