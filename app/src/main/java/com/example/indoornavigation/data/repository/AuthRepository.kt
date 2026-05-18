package com.example.indoornavigation.data.repository

import android.content.Context
import com.example.indoornavigation.data.local.NetworkMonitor
import com.example.indoornavigation.data.local.SessionManager
import com.example.indoornavigation.data.model.LoginRequest
import com.example.indoornavigation.data.model.RegisterRequest
import com.example.indoornavigation.data.model.UserResponse
import com.example.indoornavigation.data.remote.ApiService


class AuthRepository(
    private val api: ApiService,
    private val context: android.content.Context? = null,
    private val isOnline: Boolean? = null
) {
    private val net get() = isOnline ?: NetworkMonitor(context!!).isConnected

    suspend fun sendVerificationCode(
        username: String,
        email: String,
        password: String
    ): Result<Unit> {
        if (!net) return Result.failure(Exception("Нет подключения к интернету"))

        if (username.isBlank() || email.isBlank() || password.isBlank())
            return Result.failure(Exception("Заполните все поля"))
        if (password.length < 6)
            return Result.failure(Exception("Пароль минимум 6 символов"))
        if (!email.contains("@"))
            return Result.failure(Exception("Неверный формат email"))

        return runCatching {
            val res = api.registerSendCode(RegisterRequest(username, email, password))
            if (!res.success) throw Exception(res.error ?: "Ошибка отправки кода")
        }.mapFailure()
    }

    suspend fun registerDirect(
        username: String,
        email: String,
        password: String
    ): Result<UserResponse> {
        if (!net) return Result.failure(Exception("Нет подключения к интернету"))

        if (username.isBlank() || email.isBlank() || password.isBlank())
            return Result.failure(Exception("Заполните все поля"))
        if (password.length < 6)
            return Result.failure(Exception("Пароль минимум 6 символов"))
        if (!email.contains("@"))
            return Result.failure(Exception("Неверный формат email"))

        return runCatching {
            api.registerDirect(RegisterRequest(username, email, password))
        }.mapFailure()
    }

    suspend fun verifyAndRegister(
        username: String,
        email: String,
        password: String,
        code: String
    ): Result<UserResponse> {
        if (!net) return Result.failure(Exception("Нет подключения к интернету"))
        if (code.isBlank()) return Result.failure(Exception("Введите код"))

        return runCatching {
            api.registerVerify(com.example.indoornavigation.data.model.VerifyRequest(username, email, password, code))
        }.mapFailure()
    }

    suspend fun login(email: String, password: String): Result<UserResponse> {
        if (!net) return Result.failure(Exception("Нет подключения к интернету"))

        if (email.isBlank() || password.isBlank())
            return Result.failure(Exception("Заполните все поля"))

        return runCatching {
            api.login(LoginRequest(email, password))
        }.mapFailure()
    }

    private fun <T> Result<T>.mapFailure(): Result<T> =
        fold(
            onSuccess = { Result.success(it) },
            onFailure = {
                val msg = it.message ?: "Ошибка сервера"
                Result.failure(Exception(
                    when {
                        msg.contains("404") || msg.contains("не найден", ignoreCase = true) ->
                            "Пользователь не найден"
                        msg.contains("401") || msg.contains("пароль", ignoreCase = true) ->
                            "Неверный пароль"
                        msg.contains("409") || msg.contains("зарегистрирован", ignoreCase = true) ->
                            "Email уже зарегистрирован"
                        else -> msg
                    }
                ))
            }
        )
}