package ca.ramzan.delist.screens.collection_list

import androidx.lifecycle.ViewModel
import ca.ramzan.delist.room.CollectionDatabaseDao
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class CollectionListViewModel @Inject constructor(dao: CollectionDatabaseDao) : ViewModel() {

    val collections = dao.getCollectionDisplays()

}