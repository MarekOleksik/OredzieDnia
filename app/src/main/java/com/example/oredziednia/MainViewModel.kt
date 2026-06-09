package com.example.oredziednia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(
    private val repository: ApparitionRepository = SupabaseApparitionRepository()
) : ViewModel() {
    private val _currentApparition = MutableStateFlow<Apparition?>(null)
    val currentApparition: StateFlow<Apparition?> = _currentApparition.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessageRes = MutableStateFlow<Int?>(null)
    val errorMessageRes: StateFlow<Int?> = _errorMessageRes.asStateFlow()

    fun getRandomApparition() {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessageRes.value = null
            try {
                val list = repository.getAll()
                if (list.isNotEmpty()) {
                    _currentApparition.value = list.random()
                } else {
                    _errorMessageRes.value = R.string.error_no_apparitions
                }
            } catch (e: Exception) {
                _errorMessageRes.value = R.string.error_fetch_failed
            } finally {
                _isLoading.value = false
            }
        }
    }
}
