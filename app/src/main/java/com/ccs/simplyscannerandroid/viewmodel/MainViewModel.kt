package com.ccs.simplyscannerandroid.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ccs.simplyscannerandroid.data.model.ScanItem
import com.ccs.simplyscannerandroid.data.service.ScanService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {
    
    private val scanService = ScanService(application.applicationContext)
    
    private val _scanItems = MutableStateFlow<List<ScanItem>>(emptyList())
    val scanItems: StateFlow<List<ScanItem>> = _scanItems.asStateFlow()
    
    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()
    
    init {
        loadScanItems()
    }
    
    private fun loadScanItems() {
        viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            
            try {
                scanService.getAllItems()
                    .onSuccess { items ->
                        _scanItems.value = items.filter { it.deletedDate == null }
                            .sortedByDescending { it.updatedDate }
                    }
                    .onFailure { exception ->
                        _error.value = exception.message ?: "Failed to load scan items"
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun refreshItems() {
        loadScanItems()
    }
    
    fun createDocument(displayName: String) {
        viewModelScope.launch {
            try {
                scanService.createDocument(displayName)
                    .onSuccess {
                        loadScanItems() // Refresh the list
                    }
                    .onFailure { exception ->
                        _error.value = exception.message ?: "Failed to create document"
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            }
        }
    }
    
    fun createFolder(displayName: String) {
        viewModelScope.launch {
            try {
                scanService.createFolder(displayName)
                    .onSuccess {
                        loadScanItems() // Refresh the list
                    }
                    .onFailure { exception ->
                        _error.value = exception.message ?: "Failed to create folder"
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            }
        }
    }
    
    fun deleteItem(item: ScanItem) {
        viewModelScope.launch {
            try {
                scanService.deleteItem(item.uuid)
                    .onSuccess {
                        loadScanItems() // Refresh the list
                    }
                    .onFailure { exception ->
                        _error.value = exception.message ?: "Failed to delete item"
                    }
            } catch (e: Exception) {
                _error.value = e.message ?: "An unexpected error occurred"
            }
        }
    }
    
    fun clearError() {
        _error.value = null
    }
}