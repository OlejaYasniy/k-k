package com.example.indoornavigation

import com.example.indoornavigation.data.model.BasicResponse
import com.example.indoornavigation.data.model.LoginRequest
import com.example.indoornavigation.data.model.RegisterRequest
import com.example.indoornavigation.data.model.UserResponse
import com.example.indoornavigation.data.model.VerifyRequest
import com.example.indoornavigation.data.remote.ApiService
import com.example.indoornavigation.data.repository.AuthRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.*


class AuthRepositoryTest {

    private lateinit var api: ApiService
    private lateinit var repo: AuthRepository

    
    private val username = "testuser"
    private val email    = "test@example.com"
    private val password = "secret123"
    private val code     = "123456"

    @Before
    fun setup() {
        api = mock()
        
        repo = AuthRepository(api, isOnline = true)
    }

    

    @Test
    fun `sendVerificationCode возвращает ошибку если нет сети`() = runTest {
        val offlineRepo = AuthRepository(api, isOnline = false)
        val result = offlineRepo.sendVerificationCode(username, email, password)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("подключения", ignoreCase = true))
    }

    @Test
    fun `sendVerificationCode возвращает ошибку для пустого username`() = runTest {
        val result = repo.sendVerificationCode("", email, password)
        assertTrue(result.isFailure)
        verifyNoInteractions(api)
    }

    @Test
    fun `sendVerificationCode возвращает ошибку для пустого email`() = runTest {
        val result = repo.sendVerificationCode(username, "", password)
        assertTrue(result.isFailure)
        verifyNoInteractions(api)
    }

    @Test
    fun `sendVerificationCode возвращает ошибку для пустого пароля`() = runTest {
        val result = repo.sendVerificationCode(username, email, "")
        assertTrue(result.isFailure)
        verifyNoInteractions(api)
    }

    @Test
    fun `sendVerificationCode возвращает ошибку если пароль короче 6 символов`() = runTest {
        val result = repo.sendVerificationCode(username, email, "abc")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("6", ignoreCase = true))
        verifyNoInteractions(api)
    }

    @Test
    fun `sendVerificationCode возвращает ошибку для невалидного email (без @)`() = runTest {
        val result = repo.sendVerificationCode(username, "not-an-email", password)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("email", ignoreCase = true))
        verifyNoInteractions(api)
    }

    @Test
    fun `sendVerificationCode успешен при валидных данных`() = runTest {
        whenever(api.registerSendCode(any())).thenReturn(BasicResponse(success = true, message = "Код отправлен"))
        val result = repo.sendVerificationCode(username, email, password)
        assertTrue(result.isSuccess)
        verify(api).registerSendCode(RegisterRequest(username, email, password))
    }

    @Test
    fun `sendVerificationCode возвращает ошибку если сервер ответил success=false`() = runTest {
        whenever(api.registerSendCode(any())).thenReturn(BasicResponse(success = false, error = "Email уже занят"))
        val result = repo.sendVerificationCode(username, email, password)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Email уже занят"))
    }

    

    @Test
    fun `verifyAndRegister возвращает ошибку если нет сети`() = runTest {
        val offlineRepo = AuthRepository(api, isOnline = false)
        val result = offlineRepo.verifyAndRegister(username, email, password, code)
        assertTrue(result.isFailure)
    }

    @Test
    fun `verifyAndRegister возвращает ошибку для пустого кода`() = runTest {
        val result = repo.verifyAndRegister(username, email, password, "")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("код", ignoreCase = true))
        verifyNoInteractions(api)
    }

    @Test
    fun `verifyAndRegister возвращает ошибку для кода из пробелов`() = runTest {
        val result = repo.verifyAndRegister(username, email, password, "   ")
        assertTrue(result.isFailure)
        verifyNoInteractions(api)
    }

    @Test
    fun `verifyAndRegister успешен при правильном коде`() = runTest {
        val userResponse = UserResponse(id = 42, username = username, email = email)
        whenever(api.registerVerify(any())).thenReturn(userResponse)

        val result = repo.verifyAndRegister(username, email, password, code)
        assertTrue(result.isSuccess)
        assertEquals(42,       result.getOrNull()!!.id)
        assertEquals(username, result.getOrNull()!!.username)
        assertEquals(email,    result.getOrNull()!!.email)
        verify(api).registerVerify(VerifyRequest(username, email, password, code))
    }

    @Test
    fun `verifyAndRegister возвращает ошибку при бросании исключения сервером`() = runTest {
        whenever(api.registerVerify(any())).thenThrow(RuntimeException("Неверный код"))
        val result = repo.verifyAndRegister(username, email, password, "000000")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("Неверный код"))
    }

    

    @Test
    fun `login возвращает ошибку если нет сети`() = runTest {
        val offlineRepo = AuthRepository(api, isOnline = false)
        val result = offlineRepo.login(email, password)
        assertTrue(result.isFailure)
    }

    @Test
    fun `login возвращает ошибку для пустого email`() = runTest {
        val result = repo.login("", password)
        assertTrue(result.isFailure)
        verifyNoInteractions(api)
    }

    @Test
    fun `login возвращает ошибку для пустого пароля`() = runTest {
        val result = repo.login(email, "")
        assertTrue(result.isFailure)
        verifyNoInteractions(api)
    }

    @Test
    fun `login успешен при правильных данных`() = runTest {
        val userResponse = UserResponse(id = 1, username = username, email = email)
        whenever(api.login(any())).thenReturn(userResponse)

        val result = repo.login(email, password)
        assertTrue(result.isSuccess)
        assertEquals(username, result.getOrNull()!!.username)
        verify(api).login(LoginRequest(email, password))
    }

    @Test
    fun `login возвращает ошибку при неверном пароле (исключение от API)`() = runTest {
        whenever(api.login(any())).thenThrow(RuntimeException("Неверный пароль"))
        val result = repo.login(email, "wrongpassword")
        assertTrue(result.isFailure)
    }
}
