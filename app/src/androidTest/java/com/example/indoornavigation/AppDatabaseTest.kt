package com.example.indoornavigation

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.indoornavigation.data.local.AppDatabase
import com.example.indoornavigation.data.local.entity.FavoriteEntity
import com.example.indoornavigation.data.local.entity.SearchHistoryEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
class AppDatabaseTest {

    private lateinit var db: AppDatabase

    @Before
    fun createDb() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun closeDb() {
        db.close()
    }

    

    @Test
    fun favoriteDao_insertAndFind_returnsEntity() = runBlocking {
        val fav = FavoriteEntity(
            userId          = 1,
            buildingId      = 10,
            buildingName    = "Корпус А",
            buildingAddress = "ул. Примерная, 1"
        )
        db.favoriteDao().insert(fav)

        val found = db.favoriteDao().find(userId = 1, buildingId = 10)
        assertNotNull("Должен найти вставленное избранное", found)
        assertEquals("Корпус А", found!!.buildingName)
        assertEquals("ул. Примерная, 1", found.buildingAddress)
    }

    @Test
    fun favoriteDao_findForDifferentUser_returnsNull() = runBlocking {
        val fav = FavoriteEntity(
            userId = 1, buildingId = 10,
            buildingName = "Корпус А", buildingAddress = "ул. 1"
        )
        db.favoriteDao().insert(fav)

        val found = db.favoriteDao().find(userId = 2, buildingId = 10)
        assertNull("Для другого userId должен вернуть null", found)
    }

    @Test
    fun favoriteDao_delete_removesEntity() = runBlocking {
        val fav = FavoriteEntity(
            userId = 1, buildingId = 10,
            buildingName = "Корпус А", buildingAddress = "ул. 1"
        )
        db.favoriteDao().insert(fav)
        val inserted = db.favoriteDao().find(1, 10)!!

        db.favoriteDao().delete(inserted)

        val found = db.favoriteDao().find(1, 10)
        assertNull("После удаления запись не должна найтись", found)
    }

    @Test
    fun favoriteDao_getAllByUser_returnsOnlyUserFavorites() = runBlocking {
        db.favoriteDao().insert(FavoriteEntity(userId = 1, buildingId = 10, buildingName = "Корпус А", buildingAddress = "ул. 1"))
        db.favoriteDao().insert(FavoriteEntity(userId = 1, buildingId = 20, buildingName = "Корпус Б", buildingAddress = "ул. 2"))
        db.favoriteDao().insert(FavoriteEntity(userId = 2, buildingId = 30, buildingName = "Корпус В", buildingAddress = "ул. 3"))

        val favs = db.favoriteDao().getByUser(1).first()
        assertEquals("Для userId=1 должно быть 2 записи", 2, favs.size)
        assertTrue(favs.all { it.userId == 1 })
    }

    @Test
    fun favoriteDao_replaceOnDuplicateInsert() = runBlocking {
        db.favoriteDao().insert(FavoriteEntity(userId = 1, buildingId = 10, buildingName = "Старое", buildingAddress = "ул. 1"))
        db.favoriteDao().insert(FavoriteEntity(userId = 1, buildingId = 10, buildingName = "Новое", buildingAddress = "ул. 1"))

        val all = db.favoriteDao().getByUser(1).first()
        
        assertEquals("Дублирование не должно создавать несколько записей", 1, all.size)
    }

    

    @Test
    fun historyDao_insertAndGetAll_returnsHistory() = runBlocking {
        val entry = SearchHistoryEntity(
            userId       = 1,
            fromRoomName = "Аудитория 101",
            toRoomName   = "Кафетерий",
            buildingName = "Корпус В"
        )
        db.searchHistoryDao().insert(entry)

        val history = db.searchHistoryDao().getByUser(1).first()
        assertEquals("Должна быть 1 запись в истории", 1, history.size)
        assertEquals("Аудитория 101", history.first().fromRoomName)
        assertEquals("Кафетерий",     history.first().toRoomName)
    }

    @Test
    fun historyDao_getByUser_returnsOnlyUserHistory() = runBlocking {
        db.searchHistoryDao().insert(SearchHistoryEntity(1, "101", "102", "Корпус А"))
        db.searchHistoryDao().insert(SearchHistoryEntity(1, "201", "202", "Корпус А"))
        db.searchHistoryDao().insert(SearchHistoryEntity(2, "301", "302", "Корпус Б"))

        val historyUser1 = db.searchHistoryDao().getByUser(1).first()
        val historyUser2 = db.searchHistoryDao().getByUser(2).first()

        assertEquals("userId=1 должен видеть 2 записи", 2, historyUser1.size)
        assertEquals("userId=2 должен видеть 1 запись",  1, historyUser2.size)
    }

    @Test
    fun historyDao_emptyHistory_returnsEmptyList() = runBlocking {
        val history = db.searchHistoryDao().getByUser(999).first()
        assertTrue("История несуществующего пользователя должна быть пустой", history.isEmpty())
    }

    @Test
    fun historyDao_multipleInserts_keepsAllEntries() = runBlocking {
        repeat(5) { i ->
            db.searchHistoryDao().insert(
                SearchHistoryEntity(1, "Старт $i", "Конец $i", "Корпус A")
            )
        }
        val history = db.searchHistoryDao().getByUser(1).first()
        assertEquals("В истории должно быть 5 записей", 5, history.size)
    }

    @Test
    fun historyDao_hasCorrectBuildingName() = runBlocking {
        db.searchHistoryDao().insert(
            SearchHistoryEntity(1, "А", "Б", "МИРЭА Корпус В-78")
        )
        val history = db.searchHistoryDao().getByUser(1).first()
        assertEquals("МИРЭА Корпус В-78", history.first().buildingName)
    }
}
