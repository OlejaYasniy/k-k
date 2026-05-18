package com.example.indoornavigation

import com.example.indoornavigation.data.model.CrossFloorEdge
import com.example.indoornavigation.data.model.Edge
import com.example.indoornavigation.data.model.Node
import com.example.indoornavigation.routing.RouteEngine
import org.junit.Assert.*
import org.junit.Test

class RouteEngineTest {

    private val nodesFloor1 = listOf(
        Node(1, 1, 80f,  50f),
        Node(2, 1, 260f, 70f),
        Node(3, 1, 70f,  160f),
        Node(4, 1, 190f, 180f),
        Node(5, 1, 310f, 180f),
        Node(6, 1, 420f, 50f),
        Node(7, 1, 420f, 140f, "stairs")
    )

    private val nodesFloor2 = listOf(
        Node(8,  2, 260f, 70f),
        Node(9,  2, 70f,  160f),
        Node(10, 2, 190f, 180f),
        Node(11, 2, 310f, 180f),
        Node(12, 2, 420f, 140f, "stairs")
    )

    private val allNodes = nodesFloor1 + nodesFloor2

    private val edgesFloor1 = listOf(
        Edge(1, 2, 180f),
        Edge(1, 3, 110f),
        Edge(2, 4, 120f),
        Edge(2, 6, 160f),
        Edge(3, 4, 120f),
        Edge(4, 5, 120f),
        Edge(5, 7, 60f),
        Edge(6, 7, 90f)
    )

    private val edgesFloor2 = listOf(
        Edge(8,  10, 120f),
        Edge(9,  10, 120f),
        Edge(10, 11, 120f),
        Edge(11, 12, 60f)
    )

    private val allEdges = edgesFloor1 + edgesFloor2

    private val crossFloorEdges = listOf(
        CrossFloorEdge(
            fromNodeId  = 7,
            fromFloorId = 1,
            toNodeId    = 12,
            toFloorId   = 2,
            type        = "stairs",
            weight      = 50f
        )
    )

    

    @Test
    fun findPathreturnspathonsamefloor() {
        val engine = RouteEngine(allNodes, allEdges, crossFloorEdges)
        val path = engine.findPath(1, 5)
        assertTrue("Путь не должен быть пустым", path.isNotEmpty())
        assertEquals("Первый узел должен быть 1", 1, path.first().id)
        assertEquals("Последний узел должен быть 5", 5, path.last().id)
    }

    @Test
    fun findPathreturnspathacrossfloors() {
        val engine = RouteEngine(allNodes, allEdges, crossFloorEdges)
        val path = engine.findPath(1, 9)
        assertTrue("Путь не должен быть пустым", path.isNotEmpty())
        assertEquals("Первый узел должен быть 1", 1, path.first().id)
        assertEquals("Последний узел должен быть 9", 9, path.last().id)
        assertTrue(
            "Путь должен проходить через узел лестницы (id=7 или id=12)",
            path.any { it.id == 7 || it.id == 12 }
        )
    }

    @Test
    fun findPathreturnsemptylistwhennopathexists() {
        val isolatedNode = Node(99, 1, 500f, 500f)
        val engine = RouteEngine(allNodes + isolatedNode, allEdges, crossFloorEdges)
        val path = engine.findPath(1, 99)
        assertTrue("Путь должен быть пустым", path.isEmpty())
    }

    @Test
    fun findPathtoitselfreturnssinglenode() {
        val engine = RouteEngine(allNodes, allEdges, crossFloorEdges)
        val path = engine.findPath(3, 3)
        assertEquals("Путь к себе должен содержать один узел", 1, path.size)
        assertEquals(3, path.first().id)
    }

    

    @Test
    fun nearestNodereturnscorrectnode() {
        val engine = RouteEngine(allNodes, allEdges)
        val nearest = engine.nearestNode(82f, 52f)
        assertNotNull(nearest)
        assertEquals("Ближайший узел должен быть id=1", 1, nearest!!.id)
    }

    @Test
    fun nearestNodereturnsnullforemptynodelist() {
        val engine = RouteEngine(emptyList(), emptyList())
        val nearest = engine.nearestNode(100f, 100f)
        assertNull("При пустом списке должен вернуться null", nearest)
    }

    

    @Test
    fun pathLengthcalculatescorrectdistance() {
        val nodes = listOf(
            Node(1, 1, 0f, 0f),
            Node(2, 1, 3f, 4f)
        )
        val engine = RouteEngine(nodes, emptyList())
        val length = engine.pathLength(nodes)
        assertEquals("Длина пути 3-4-5 должна быть 5.0", 5.0f, length, 0.01f)
    }

    @Test
    fun pathLengthreturnszeroforsinglenode() {
        val nodes = listOf(Node(1, 1, 10f, 10f))
        val engine = RouteEngine(nodes, emptyList())
        val length = engine.pathLength(nodes)
        assertEquals("Длина пути из одного узла должна быть 0", 0f, length, 0.01f)
    }

    @Test
    fun pathLengthreturnszeroforemptypath() {
        val engine = RouteEngine(emptyList(), emptyList())
        val length = engine.pathLength(emptyList())
        assertEquals(0f, length, 0.01f)
    }
}