package com.example.indoornavigation.ui.common

import android.content.Context

object LocalizationHelper {
    fun localizeAddress(address: String, context: Context): String {
        val locale = context.resources.configuration.locales[0].language
        val isEn = locale == "en"
        return when {
            address.contains("Пушкина", ignoreCase = true) || address.contains("Pushkin", ignoreCase = true) -> {
                if (isEn) "10 Pushkin St." else "ул. Пушкина, 10"
            }
            address.contains("Лермонтова", ignoreCase = true) || address.contains("Lermontov", ignoreCase = true) -> {
                if (isEn) "15 Lermontov St." else "ул. Лермонтова, 15"
            }
            address.contains("Чехова", ignoreCase = true) || address.contains("Chekhov", ignoreCase = true) -> {
                if (isEn) "5 Chekhov St." else "ул. Чехова, 5"
            }
            address.contains("Родионова", ignoreCase = true) || address.contains("Rodionova", ignoreCase = true) -> {
                if (isEn) "187 Rodionova St." else "ул. Родионова, 187"
            }
            else -> address
        }
    }

    fun localizeName(name: String, context: Context): String {
        val locale = context.resources.configuration.locales[0].language
        val isEn = locale == "en"

        return when {
            // === Buildings ===
            name.equals("Главный корпус", ignoreCase = true) || name.equals("Main Building", ignoreCase = true) -> {
                if (isEn) "Main Building" else "Главный корпус"
            }
            name.equals("Учебный корпус Б", ignoreCase = true) || name.equals("Building B", ignoreCase = true) || name.equals("Учебный корпус B", ignoreCase = true) -> {
                if (isEn) "Building B" else "Учебный корпус Б"
            }
            name.equals("Спортивный комплекс", ignoreCase = true) || name.equals("Sports Complex", ignoreCase = true) -> {
                if (isEn) "Sports Complex" else "Спортивный комплекс"
            }
            name.equals("ТРК Фантастика", ignoreCase = true) || name.equals("TRK Fantastika", ignoreCase = true) -> {
                if (isEn) "TRK Fantastika" else "ТРК Фантастика"
            }

            // === Floors ===
            name.contains("этаж", ignoreCase = true) || name.contains("Floor", ignoreCase = true) -> {
                val num = name.find { it.isDigit() }?.toString() ?: ""
                if (isEn) {
                    val suffix = when (num) {
                        "1" -> "st"
                        "2" -> "nd"
                        "3" -> "rd"
                        else -> "th"
                    }
                    if (num.isNotEmpty()) "${num}${suffix} Floor" else "Floor"
                } else {
                    if (num.isNotEmpty()) "$num этаж" else "Этаж"
                }
            }

            // === Standard Rooms & POIs ===
            name.equals("Вход", ignoreCase = true) || name.equals("Entrance", ignoreCase = true) -> {
                if (isEn) "Entrance" else "Вход"
            }
            name.equals("Выход", ignoreCase = true) || name.equals("Exit", ignoreCase = true) -> {
                if (isEn) "Exit" else "Выход"
            }
            name.equals("Туалет", ignoreCase = true) || name.equals("WC", ignoreCase = true) || name.equals("Restroom", ignoreCase = true) -> {
                if (isEn) "Toilet" else "Туалет"
            }
            name.equals("Лестница", ignoreCase = true) || name.equals("Stairs", ignoreCase = true) -> {
                if (isEn) "Stairs" else "Лестница"
            }
            name.equals("Лифт", ignoreCase = true) || name.equals("Elevator", ignoreCase = true) -> {
                if (isEn) "Elevator" else "Лифт"
            }
            name.equals("Эскалатор", ignoreCase = true) || name.equals("Escalator", ignoreCase = true) -> {
                if (isEn) "Escalator" else "Эскалатор"
            }
            name.equals("Деканат", ignoreCase = true) || name.equals("Dean's Office", ignoreCase = true) -> {
                if (isEn) "Dean's Office" else "Деканат"
            }
            name.equals("Кафедра", ignoreCase = true) || name.equals("Department", ignoreCase = true) -> {
                if (isEn) "Department" else "Кафедра"
            }
            name.equals("Библиотека", ignoreCase = true) || name.equals("Library", ignoreCase = true) -> {
                if (isEn) "Library" else "Библиотека"
            }
            name.equals("Столовая", ignoreCase = true) || name.equals("Cafeteria", ignoreCase = true) -> {
                if (isEn) "Cafeteria" else "Столовая"
            }
            name.equals("Гардероб", ignoreCase = true) || name.equals("Wardrobe", ignoreCase = true) -> {
                if (isEn) "Wardrobe" else "Гардероб"
            }
            name.equals("Буфет", ignoreCase = true) || name.equals("Buffet", ignoreCase = true) -> {
                if (isEn) "Buffet" else "Буфет"
            }
            name.equals("Холл", ignoreCase = true) || name.equals("Hall", ignoreCase = true) -> {
                if (isEn) "Hall" else "Холл"
            }
            name.equals("Коридор", ignoreCase = true) || name.equals("Corridor", ignoreCase = true) -> {
                if (isEn) "Corridor" else "Коридор"
            }

            // === Specific Rooms from Seed ===
            name.equals("Туалет М", ignoreCase = true) || name.equals("Restroom M", ignoreCase = true) || name.equals("Toilet M", ignoreCase = true) -> {
                if (isEn) "Restroom M" else "Туалет М"
            }
            name.equals("Туалет Ж", ignoreCase = true) || name.equals("Restroom W", ignoreCase = true) || name.equals("Toilet W", ignoreCase = true) -> {
                if (isEn) "Restroom W" else "Туалет Ж"
            }
            name.equals("Раздевалка М", ignoreCase = true) || name.equals("Locker Room M", ignoreCase = true) -> {
                if (isEn) "Locker Room M" else "Раздевалка М"
            }
            name.equals("Раздевалка Ж", ignoreCase = true) || name.equals("Locker Room W", ignoreCase = true) -> {
                if (isEn) "Locker Room W" else "Раздевалка Ж"
            }
            name.equals("Серверная", ignoreCase = true) || name.equals("Server Room", ignoreCase = true) -> {
                if (isEn) "Server Room" else "Серверная"
            }
            name.equals("Архив", ignoreCase = true) || name.equals("Archive", ignoreCase = true) -> {
                if (isEn) "Archive" else "Архив"
            }
            name.equals("Ресепшн", ignoreCase = true) || name.equals("Reception", ignoreCase = true) -> {
                if (isEn) "Reception" else "Ресепшн"
            }
            name.equals("Спортзал", ignoreCase = true) || name.equals("Gym", ignoreCase = true) -> {
                if (isEn) "Gym" else "Спортзал"
            }
            name.equals("Тренажёрный", ignoreCase = true) || name.equals("Fitness Room", ignoreCase = true) -> {
                if (isEn) "Fitness Room" else "Тренажёрный"
            }
            name.equals("Бассейн", ignoreCase = true) || name.equals("Pool", ignoreCase = true) -> {
                if (isEn) "Pool" else "Бассейн"
            }
            name.equals("Вход Б", ignoreCase = true) || name.equals("Entrance B", ignoreCase = true) -> {
                if (isEn) "Entrance B" else "Вход Б"
            }
            name.equals("Холл Б-2", ignoreCase = true) || name.equals("Hall B-2", ignoreCase = true) -> {
                if (isEn) "Hall B-2" else "Холл Б-2"
            }
            name.equals("Охрана", ignoreCase = true) || name.equals("Security", ignoreCase = true) -> {
                if (isEn) "Security" else "Охрана"
            }
            name.equals("Ашан", ignoreCase = true) || name.equals("Auchan", ignoreCase = true) -> {
                if (isEn) "Auchan" else "Ашан"
            }
            name.equals("Золотое Яблоко", ignoreCase = true) || name.equals("Gold Apple", ignoreCase = true) -> {
                if (isEn) "Gold Apple" else "Золотое Яблоко"
            }
            name.equals("М.Видео", ignoreCase = true) || name.equals("M.Video", ignoreCase = true) -> {
                if (isEn) "M.Video" else "М.Видео"
            }
            name.equals("Спортмастер", ignoreCase = true) || name.equals("Sportmaster", ignoreCase = true) -> {
                if (isEn) "Sportmaster" else "Спортмастер"
            }
            name.equals("Синема Парк", ignoreCase = true) || name.equals("Cinema Park", ignoreCase = true) -> {
                if (isEn) "Cinema Park" else "Синема Парк"
            }
            name.equals("ФизКульт", ignoreCase = true) || name.equals("FizKult Fitness", ignoreCase = true) -> {
                if (isEn) "FizKult Fitness" else "ФизКульт"
            }
            name.equals("Вкусно — и точка", ignoreCase = true) || name.equals("Vkusno i Tochka", ignoreCase = true) -> {
                if (isEn) "Vkusno i Tochka" else "Вкусно — и точка"
            }
            name.equals("Вход 1", ignoreCase = true) || name.equals("Entrance 1", ignoreCase = true) -> {
                if (isEn) "Entrance 1" else "Вход 1"
            }
            name.equals("Вход 2", ignoreCase = true) || name.equals("Entrance 2", ignoreCase = true) -> {
                if (isEn) "Entrance 2" else "Вход 2"
            }
            name.equals("Инфо-стойка", ignoreCase = true) || name.equals("Info Desk", ignoreCase = true) -> {
                if (isEn) "Info Desk" else "Инфо-стойка"
            }
            name.equals("Косметика", ignoreCase = true) || name.equals("Cosmetics", ignoreCase = true) -> {
                if (isEn) "Cosmetics" else "Косметика"
            }
            name.equals("Техника", ignoreCase = true) || name.equals("Electronics", ignoreCase = true) -> {
                if (isEn) "Electronics" else "Техника"
            }
            name.equals("Кинотеатр", ignoreCase = true) || name.equals("Cinema", ignoreCase = true) -> {
                if (isEn) "Cinema" else "Кинотеатр"
            }
            name.equals("Фуд-корт", ignoreCase = true) || name.equals("Food Court", ignoreCase = true) -> {
                if (isEn) "Food Court" else "Фуд-корт"
            }

            // === Dynamic Prefixes ===
            name.startsWith("Комната", ignoreCase = true) -> {
                if (isEn) name.replace("Комната", "Room", ignoreCase = true) else name
            }
            name.startsWith("Кабинет", ignoreCase = true) -> {
                if (isEn) name.replace("Кабинет", "Office", ignoreCase = true) else name
            }
            name.startsWith("Каб.", ignoreCase = true) -> {
                if (isEn) name.replace("Каб.", "Office", ignoreCase = true) else name
            }
            name.startsWith("ауд.", ignoreCase = true) -> {
                if (isEn) name.replace("ауд.", "Room", ignoreCase = true) else name
            }
            name.startsWith("аудитория", ignoreCase = true) -> {
                if (isEn) name.replace("аудитория", "Room", ignoreCase = true) else name
            }
            name.startsWith("Room", ignoreCase = true) -> {
                if (!isEn) name.replace("Room", "Комната", ignoreCase = true) else name
            }
            name.startsWith("Office", ignoreCase = true) -> {
                if (!isEn) name.replace("Office", "Каб.", ignoreCase = true) else name
            }
            else -> name
        }
    }
}
