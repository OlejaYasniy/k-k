package com.example.indoornavigation

import com.example.indoornavigation.data.model.Node
import com.example.indoornavigation.routing.InstructionEngine
import org.junit.Assert.*
import org.junit.Test

class InstructionEngineTest {

    private val engine = InstructionEngine()

    @Test
    fun buildStepsreturnsemptylistforemptypath() {
        val steps = engine.buildSteps(emptyList())
        assertTrue("Шаги для пустого пути должны быть пустыми", steps.isEmpty())
    }

    @Test
    fun buildStepsreturnsSTARTandARRIVEforsinglenode() {
        val path = listOf(Node(1, 1, 0f, 0f))
        val steps = engine.buildSteps(path)
        assertEquals(2, steps.size)
        assertEquals("START", steps.first().type)
        assertEquals("ARRIVE", steps.last().type)
    }

    @Test
    fun buildStepscontainsSTRAIGHTstepfordistantnodes() {
        val path = listOf(
            Node(1, 1, 0f,   0f),
            Node(2, 1, 100f, 0f)
        )
        val steps = engine.buildSteps(path)
        assertTrue(
            "Должен быть шаг STRAIGHT",
            steps.any { it.type == "STRAIGHT" }
        )
    }

    @Test
    fun buildStepscontainsTURN_LEFTforleftturn() {
        
        val path = listOf(
            Node(1, 1, 0f,   0f),
            Node(2, 1, 100f, 0f),
            Node(3, 1, 100f, -100f)
        )
        val steps = engine.buildSteps(path)
        assertTrue(
            "Должен быть шаг TURN_LEFT",
            steps.any { it.type == "TURN_LEFT" }
        )
    }

    @Test
    fun buildStepscontainsTURN_RIGHTforrightturn() {
        
        val path = listOf(
            Node(1, 1, 0f,   0f),
            Node(2, 1, 100f, 0f),
            Node(3, 1, 100f, 100f)
        )
        val steps = engine.buildSteps(path)
        assertTrue(
            "Должен быть шаг TURN_RIGHT",
            steps.any { it.type == "TURN_RIGHT" }
        )
    }

    @Test
    fun buildStepscontainsFLOORstepgoingup() {
        val path = listOf(
            Node(7,  1, 420f, 140f, "stairs"),
            Node(12, 2, 420f, 140f, "stairs")
        )
        val steps = engine.buildSteps(path)
        val floorStep = steps.find { it.type == "FLOOR" }
        assertNotNull("Должен быть шаг FLOOR", floorStep)
        assertTrue(
            "Текст должен содержать 'Поднимитесь'",
            floorStep!!.text.contains("Поднимитесь")
        )
    }

    @Test
    fun buildStepscontainsFLOORstepgoingdown() {
        val path = listOf(
            Node(12, 2, 420f, 140f, "stairs"),
            Node(7,  1, 420f, 140f, "stairs")
        )
        val steps = engine.buildSteps(path)
        val floorStep = steps.find { it.type == "FLOOR" }
        assertNotNull("Должен быть шаг FLOOR", floorStep)
        assertTrue(
            "Текст должен содержать 'Спуститесь'",
            floorStep!!.text.contains("Спуститесь")
        )
    }

    @Test
    fun buildStepsalwaysstartswithSTARTandendswithARRIVE() {
        val path = listOf(
            Node(1, 1, 0f,   0f),
            Node(2, 1, 100f, 0f),
            Node(3, 1, 200f, 0f)
        )
        val steps = engine.buildSteps(path)
        assertEquals("START",  steps.first().type)
        assertEquals("ARRIVE", steps.last().type)
    }
}