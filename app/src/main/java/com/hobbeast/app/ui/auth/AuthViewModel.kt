package com.hobbeast.app.ui.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hobbeast.app.data.remote.SupabaseDataSource
import com.hobbeast.app.data.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val supabase: SupabaseDataSource,
    private val userRepository: UserRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    fun signIn(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _uiState.value = AuthUiState.Error("Kérjük töltsd ki az összes mezőt")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching { supabase.signIn(email, password) }
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Sikertelen bejelentkezés") }
        }
    }

    fun signUp(email: String, password: String, displayName: String) {
        if (email.isBlank() || password.isBlank() || displayName.isBlank()) {
            _uiState.value = AuthUiState.Error("Kérjük töltsd ki az összes mezőt")
            return
        }
        if (password.length < 8) {
            _uiState.value = AuthUiState.Error("A jelszó legalább 8 karakter legyen")
            return
        }
        viewModelScope.launch {
            _uiState.value = AuthUiState.Loading
            runCatching { supabase.signUp(email, password) }
                .onSuccess { _uiState.value = AuthUiState.Success }
                .onFailure { _uiState.value = AuthUiState.Error(it.message ?: "Sikertelen regisztráció") }
        }
    }

    fun saveInterests(interests: List<String>) {
        viewModelScope.launch {
            userRepository.saveInterests(interests)
        }
    }

    fun resetState() {
        _uiState.value = AuthUiState.Idle
    }
}

sealed interface AuthUiState {
    data object Idle    : AuthUiState
    data object Loading : AuthUiState
    data object Success : AuthUiState
    data class Error(val message: String) : AuthUiState
}
