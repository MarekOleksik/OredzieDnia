package com.example.oredziednia

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private fun yearOf(date: String): Int? = date.take(4).toIntOrNull()
private fun monthOf(date: String): Int? = date.drop(5).take(2).toIntOrNull()

class BrowseViewModel(
    private val repository: ApparitionRepository = SupabaseApparitionRepository()
) : ViewModel() {

    private val _allApparitions = MutableStateFlow<List<Apparition>>(emptyList())
    private val _isLoading = MutableStateFlow(false)
    private val _errorMessageRes = MutableStateFlow<Int?>(null)

    private val _selectedLocation = MutableStateFlow<String?>(null)
    private val _selectedYear = MutableStateFlow<Int?>(null)
    private val _selectedMonth = MutableStateFlow<Int?>(null)

    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    val errorMessageRes: StateFlow<Int?> = _errorMessageRes.asStateFlow()
    val selectedLocation: StateFlow<String?> = _selectedLocation.asStateFlow()
    val selectedYear: StateFlow<Int?> = _selectedYear.asStateFlow()
    val selectedMonth: StateFlow<Int?> = _selectedMonth.asStateFlow()

    val locations: StateFlow<List<String>> = _allApparitions
        .map { apparitions -> apparitions.map { it.location }.distinct().sorted() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val years: StateFlow<List<Int>> = combine(_allApparitions, _selectedLocation) { apparitions, location ->
        apparitions
            .asSequence()
            .filter { location == null || it.location == location }
            .mapNotNull { yearOf(it.date) }
            .distinct()
            .sortedDescending()
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val months: StateFlow<List<Int>> = combine(
        _allApparitions, _selectedLocation, _selectedYear
    ) { apparitions, location, year ->
        apparitions
            .asSequence()
            .filter { location == null || it.location == location }
            .filter { year == null || yearOf(it.date) == year }
            .mapNotNull { monthOf(it.date) }
            .distinct()
            .sorted()
            .toList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val filteredResults: StateFlow<List<Apparition>> = combine(
        _allApparitions, _selectedLocation, _selectedYear, _selectedMonth
    ) { apparitions, location, year, month ->
        apparitions
            .filter { location == null || it.location == location }
            .filter { year == null || yearOf(it.date) == year }
            .filter { month == null || monthOf(it.date) == month }
            .sortedBy { it.date }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        load()
    }

    fun load() {
        if (_isLoading.value) return

        viewModelScope.launch {
            _isLoading.value = true
            _errorMessageRes.value = null
            try {
                val list = repository.getAll()
                _allApparitions.value = list
                if (list.isEmpty()) {
                    _errorMessageRes.value = R.string.error_no_apparitions
                }
            } catch (e: Exception) {
                _errorMessageRes.value = R.string.error_fetch_failed
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun selectLocation(location: String?) {
        if (_selectedLocation.value == location) return
        _selectedLocation.value = location
        _selectedYear.value = null
        _selectedMonth.value = null
    }

    fun selectYear(year: Int?) {
        if (_selectedYear.value == year) return
        _selectedYear.value = year
        _selectedMonth.value = null
    }

    fun selectMonth(month: Int?) {
        _selectedMonth.value = month
    }
}
