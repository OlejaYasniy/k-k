package com.example.indoornavigation

import android.content.Context
import com.example.indoornavigation.data.model.Node
import com.example.indoornavigation.routing.InstructionEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever


class InstructionEngineTest {

    private lateinit var engine: InstructionEngine
    private lateinit var context: Context

    @Before
    fun setup() {
        engine = InstructionEngine()
        context = mock()

        
        whenever(context.getString(eq(R.string.instruction_start), any())).thenAnswer { inv ->
            "Начало маршрута: узел ${inv.arguments[1]}"
        }
        whenever(context.getString(eq(R.string.instruction_floor), any(), any(), any())).thenAnswer { inv ->
            "${inv.arguments[1]} на этаж ${inv.arguments[2]} ${inv.arguments[3]}"
        }
        whenever(context.getString(R.string.instruction_floor_up)).thenReturn("Поднимитесь")
        whenever(context.getString(R.string.instruction_floor_down)).thenReturn("Спуститесь")
        whenever(context.getString(R.string.instruction_via_elevator)).thenReturn("на лифте")
        whenever(context.getString(R.string.instruction_via_stairs)).thenReturn("по лестнице")
        whenever(context.getString(R.string.instruction_turn_left)).thenReturn("Поверните налево")
        whenever(context.getString(R.string.instruction_turn_right)).thenReturn("Поверните направо")
        whenever(context.getString(R.string.instruction_straight)).thenReturn("Идите прямо")
        whenever(context.getString(R.string.instruction_exit_room)).thenReturn("Выйдите в коридор")
        whenever(context.getString(R.string.instruction_enter_room)).thenReturn("Войдите в комнату")
        whenever(context.getString(R.string.instruction_arrive)).thenReturn("Вы прибыли")
    }

    

    @Test
    fun `buildSteps возвращает пустой список для пустого пути`() {
        val steps = engine.buildSteps(emptyList(), context)
        assertTrue("Для пустого пути шаги должны быть пустыми", steps.isEmpty())
    }

    @Test
    fun `buildSteps для одного узла возвращает START и ARRIVE`() {
        val path = listOf(Node(1, 1, 0f, 0f))
        val steps = engine.buildSteps(path, context)
        assertEquals("Должно быть 2 шага", 2, steps.size)
        assertEquals("START", steps.first().type)
        assertEquals("ARRIVE", steps.last().type)
    }

    

    @Test
    fun `buildSteps всегда начинается с START и заканчивается ARRIVE`() {
        val path = listOf(
            Node(1, 1,   0f, 0f),
            Node(2, 1, 100f, 0f),
            Node(3, 1, 200f, 0f)
        )
        val steps = engine.buildSteps(path, context)
        assertEquals("START",  steps.first().type)
        assertEquals("ARRIVE", steps.last().type)
    }

    @Test
    fun `buildSteps содержит шаг STRAIGHT для движения вперёд`() {
        val path = listOf(
            Node(1, 1,   0f, 0f),
            Node(2, 1, 100f, 0f)
        )
        val steps = engine.buildSteps(path, context)
        assertTrue(
            "Должен содержать шаг STRAIGHT",
            steps.any { it.type == "STRAIGHT" }
        )
    }

    

    @Test
    fun `buildSteps содержит TURN_LEFT для поворота налево`() {
        
        val path = listOf(
            Node(1, 1,   0f,    0f),
            Node(2, 1, 100f,    0f),
            Node(3, 1, 100f, -100f)
        )
        val steps = engine.buildSteps(path, context)
        assertTrue(
            "Должен содержать TURN_LEFT",
            steps.any { it.type == "TURN_LEFT" }
        )
    }

    @Test
    fun `buildSteps содержит TURN_RIGHT для поворота направо`() {
        
        val path = listOf(
            Node(1, 1,   0f,   0f),
            Node(2, 1, 100f,   0f),
            Node(3, 1, 100f, 100f)
        )
        val steps = engine.buildSteps(path, context)
        assertTrue(
            "Должен содержать TURN_RIGHT",
            steps.any { it.type == "TURN_RIGHT" }
        )
    }

    @Test
    fun `buildSteps не содержит лишних поворотов для прямого пути`() {
        
        val path = listOf(
            Node(1, 1,   0f, 0f),
            Node(2, 1, 100f, 0f),
            Node(3, 1, 200f, 0f)
        )
        val steps = engine.buildSteps(path, context)
        assertFalse(
            "Прямой путь не должен содержать TURN_LEFT",
            steps.any { it.type == "TURN_LEFT" }
        )
        assertFalse(
            "Прямой путь не должен содержать TURN_RIGHT",
            steps.any { it.type == "TURN_RIGHT" }
        )
    }

    

    @Test
    fun `buildSteps содержит FLOOR при переходе с этажа на этаж вверх`() {
        val path = listOf(
            Node(7,  1, 420f, 140f, "stairs"),
            Node(12, 2, 420f, 140f, "stairs")
        )
        val steps = engine.buildSteps(path, context)
        val floorStep = steps.find { it.type == "FLOOR" }
        assertNotNull("Должен содержать шаг FLOOR", floorStep)
        assertTrue(
            "FLOOR шаг для подъёма должен содержать слово 'поднимитесь'",
            floorStep!!.text.contains("поднимитесь", ignoreCase = true)
        )
    }

    @Test
    fun `buildSteps содержит FLOOR при переходе с этажа на этаж вниз`() {
        val path = listOf(
            Node(12, 2, 420f, 140f, "stairs"),
            Node(7,  1, 420f, 140f, "stairs")
        )
        val steps = engine.buildSteps(path, context)
        val floorStep = steps.find { it.type == "FLOOR" }
        assertNotNull("Должен содержать шаг FLOOR", floorStep)
        assertTrue(
            "FLOOR шаг для спуска должен содержать слово 'спуститесь'",
            floorStep!!.text.contains("спуститесь", ignoreCase = true)
        )
    }

    @Test
    fun `buildSteps содержит FLOOR при переходе через лифт`() {
        val path = listOf(
            Node(1, 1, 100f, 100f, "elevator"),
            Node(2, 3, 100f, 100f, "elevator")
        )
        val steps = engine.buildSteps(path, context)
        assertTrue(
            "Должен содержать FLOOR шаг для лифта",
            steps.any { it.type == "FLOOR" }
        )
    }

    

    @Test
    fun `buildSteps имеет непустой текст у каждого шага`() {
        val path = listOf(
            Node(1, 1,   0f, 0f),
            Node(2, 1, 100f, 0f),
            Node(3, 1, 100f, 100f)
        )
        val steps = engine.buildSteps(path, context)
        assertTrue("Все шаги должны иметь непустой текст", steps.all { it.text.isNotBlank() })
    }

    @Test
    fun `buildSteps для длинного пути содержит несколько шагов`() {
        val path = listOf(
            Node(1, 1,   0f,   0f),
            Node(2, 1, 100f,   0f),
            Node(3, 1, 100f, 100f),
            Node(4, 1, 200f, 100f),
            Node(5, 1, 200f, 200f)
        )
        val steps = engine.buildSteps(path, context)
        assertTrue("Длинный путь должен содержать более 2 шагов", steps.size > 2)
    }
}
