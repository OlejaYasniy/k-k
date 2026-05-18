package com.example.indoornavigation.ui.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.indoornavigation.data.local.SessionManager
import com.example.indoornavigation.data.remote.RetrofitProvider
import com.example.indoornavigation.data.repository.AuthRepository
import com.example.indoornavigation.ui.common.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class AuthViewModel(app: Application) : AndroidViewModel(app) {

    private val repo    = AuthRepository(RetrofitProvider.api, app.applicationContext)
    val session         = SessionManager(app)

    private val _authState = MutableStateFlow<UiState<String>>(UiState.Idle)
    val authState: StateFlow<UiState<String>> = _authState

    private val _codeSent = MutableStateFlow(false)
    val codeSent: StateFlow<Boolean> = _codeSent

    fun sendVerificationCode(username: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            repo.sendVerificationCode(username, email, password).fold(
                onSuccess = { 
                    _codeSent.value  = true
                    _authState.value = UiState.Idle
                },
                onFailure = { _authState.value = UiState.Error(it.message ?: "Ошибка") }
            )
        }
    }

    fun registerDirect(username: String, email: String, password: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            repo.registerDirect(username, email, password).fold(
                onSuccess = { user ->
                    session.userId   = user.id
                    session.username = user.username
                    session.email    = user.email
                    _authState.value = UiState.Success(user.username)
                },
                onFailure = { _authState.value = UiState.Error(it.message ?: "Ошибка") }
            )
        }
    }

    fun resetAuthState() {
        _authState.value = UiState.Idle
    }

    fun verifyCodeAndRegister(username: String, email: String, password: String, code: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            repo.verifyAndRegister(username, email, password, code).fold(
                onSuccess = { user ->
                    session.userId   = user.id
                    session.username = user.username
                    session.email    = user.email
                    _codeSent.value  = false
                    _authState.value = UiState.Success(user.username)
                },
                onFailure = { _authState.value = UiState.Error(it.message ?: "Ошибка") }
            )
        }
    }

    fun login(email: String, password: String) {
        viewModelScope.launch {
            _authState.value = UiState.Loading
            repo.login(email, password).fold(
                onSuccess = { user ->
                    session.userId   = user.id
                    session.username = user.username
                    session.email    = user.email
                    _authState.value = UiState.Success(user.username)
                },
                onFailure = { _authState.value = UiState.Error(it.message ?: "Ошибка") }
            )
        }
    }

    fun logout() {
        session.clear()
        _authState.value = UiState.Idle
    }

    class Factory(private val app: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>) =
            AuthViewModel(app) as T
    }
}