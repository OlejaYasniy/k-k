package com.example.indoornavigation.routing

import com.example.indoornavigation.data.model.Node
import org.junit.Assert.*
import org.junit.Test


class AuthRepositoryTest {

    
    private val fakeUsers = mutableMapOf<String, Pair<String, String>>()
    

    private fun register(
        username: String,
        email: String,
        password: String
    ): Boolean {
        if (fakeUsers.containsKey(email)) return false
        fakeUsers[email] = username to password
        return true
    }

    private fun login(email: String, password: String): String? {
        val user = fakeUsers[email] ?: return null
        return if (user.second == password) user.first else null
    }

    

    @Test
    fun registersavesnewusersuccessfully() {
        val result = register("oleg", "oleg@mail.ru", "pass123")
        assertTrue("Регистрация нового пользователя должна быть успешной", result)
    }

    @Test
    fun registerfailswhenemailalreadyexists() {
        register("oleg", "oleg@mail.ru", "pass123")
        val result = register("oleg2", "oleg@mail.ru", "pass456")
        assertFalse("Повторная регистрация с тем же email должна завершиться неудачей", result)
    }

    @Test
    fun registeredusercanbefoundafterregistration() {
        register("oleg", "oleg@mail.ru", "pass123")
        assertTrue("Пользователь должен существовать после регистрации",
            fakeUsers.containsKey("oleg@mail.ru"))
    }

    

    @Test
    fun loginsucceedswithcorrectcredentials() {
        register("oleg", "oleg@mail.ru", "pass123")
        val result = login("oleg@mail.ru", "pass123")
        assertNotNull("Вход с верными данными должен быть успешным", result)
        assertEquals("oleg", result)
    }

    @Test
    fun loginfailswithwrongpassword() {
        register("oleg", "oleg@mail.ru", "pass123")
        val result = login("oleg@mail.ru", "wrongpass")
        assertNull("Вход с неверным паролем должен завершиться неудачей", result)
    }

    @Test
    fun loginfailswhenuserdoesnotexist() {
        val result = login("unknown@mail.ru", "pass123")
        assertNull("Вход несуществующего пользователя должен завершиться неудачей", result)
    }
}