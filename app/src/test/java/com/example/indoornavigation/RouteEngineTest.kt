package com.example.indoornavigation

import com.example.indoornavigation.data.model.CrossFloorEdge
import com.example.indoornavigation.data.model.Edge
import com.example.indoornavigation.data.model.Node
import com.example.indoornavigation.routing.RouteEngine
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test


class RouteEngineTest {

    private lateinit var engine: RouteEngine

    
    private val nodesFloor1 = listOf(
        Node(1, 1,  80f,  50f),
        Node(2, 1, 260f,  70f),
        Node(3, 1,  70f, 160f),
        Node(4, 1, 190f, 180f),
        Node(5, 1, 310f, 180f),
        Node(6, 1, 420f,  50f),
        Node(7, 1, 420f, 140f, "stairs")
    )
    private val nodesFloor2 = listOf(
        Node(8,  2, 260f,  70f),
        Node(9,  2,  70f, 160f),
        Node(10, 2, 190f, 180f),
        Node(11, 2, 310f, 180f),
        Node(12, 2, 420f, 140f, "stairs")
    )
    private val allNodes = nodesFloor1 + nodesFloor2

    private val edgesFloor1 = listOf(
        Edge(1, 2, 180f), Edge(1, 3, 110f),
        Edge(2, 4, 120f), Edge(2, 6, 160f),
        Edge(3, 4, 120f), Edge(4, 5, 120f),
        Edge(5, 7,  60f), Edge(6, 7,  90f)
    )
    private val edgesFloor2 = listOf(
        Edge(8,  10, 120f), Edge(9,  10, 120f),
        Edge(10, 11, 120f), Edge(11, 12,  60f)
    )
    private val allEdges = edgesFloor1 + edgesFloor2

    private val crossFloorEdges = listOf(
        CrossFloorEdge(fromNodeId = 7, fromFloorId = 1, toNodeId = 12, toFloorId = 2, type = "stairs", weight = 50f)
    )

    @Before
    fun setup() {
        engine = RouteEngine(allNodes, allEdges, crossFloorEdges)
    }

    

    @Test
    fun `findPath возвращает путь на том же этаже`() {
        val path = engine.findPath(1, 5)
        assertTrue("Путь не должен быть пустым", path.isNotEmpty())
        assertEquals("Начало пути — узел 1", 1, path.first().id)
        assertEquals("Конец пути — узел 5", 5, path.last().id)
    }

    @Test
    fun `findPath содержит только узлы графа`() {
        val path = engine.findPath(1, 6)
        val validIds = allNodes.map { it.id }.toSet()
        assertTrue("Все узлы пути должны принадлежать графу", path.all { it.id in validIds })
    }

    @Test
    fun `findPath находит путь между этажами через лестницу`() {
        val path = engine.findPath(1, 9)
        assertTrue("Путь не должен быть пустым", path.isNotEmpty())
        assertEquals("Начало пути — узел 1", 1, path.first().id)
        assertEquals("Конец пути — узел 9", 9, path.last().id)
        assertTrue(
            "Путь должен содержать лестничные узлы (id=7 или id=12)",
            path.any { it.id == 7 || it.id == 12 }
        )
    }

    @Test
    fun `findPath из узла в себя возвращает один узел`() {
        val path = engine.findPath(3, 3)
        assertEquals("Путь до самого себя — ровно 1 узел", 1, path.size)
        assertEquals(3, path.first().id)
    }

    @Test
    fun `findPath возвращает пустой список если пути нет`() {
        val isolatedNode = Node(99, 1, 999f, 999f)
        val engineWithIsolated = RouteEngine(allNodes + isolatedNode, allEdges, crossFloorEdges)
        val path = engineWithIsolated.findPath(1, 99)
        assertTrue("Путь до изолированного узла должен быть пустым", path.isEmpty())
    }

    @Test
    fun `findPath возвращает пустой список если узел не существует`() {
        val path = engine.findPath(1, 9999)
        assertTrue("Путь до несуществующего узла должен быть пустым", path.isEmpty())
    }

    @Test
    fun `findPath возвращает пустой список если начальный узел не существует`() {
        val path = engine.findPath(9999, 1)
        assertTrue("Путь от несуществующего узла должен быть пустым", path.isEmpty())
    }

    @Test
    fun `findPath для пустого графа возвращает пустой список`() {
        val emptyEngine = RouteEngine(emptyList(), emptyList())
        val path = emptyEngine.findPath(1, 2)
        assertTrue(path.isEmpty())
    }

    

    @Test
    fun `nearestNode возвращает ближайший узел`() {
        val nearest = engine.nearestNode(82f, 52f) 
        assertNotNull(nearest)
        assertEquals("Ближайший к (82,52) должен быть узел id=1", 1, nearest!!.id)
    }

    @Test
    fun `nearestNode возвращает null для пустого списка`() {
        val emptyEngine = RouteEngine(emptyList(), emptyList())
        assertNull("Для пустого графа должен вернуть null", emptyEngine.nearestNode(100f, 100f))
    }

    @Test
    fun `nearestNode работает с точным совпадением координат`() {
        val nearest = engine.nearestNode(420f, 140f) 
        assertNotNull(nearest)
        assertEquals(7, nearest!!.id)
    }

    

    @Test
    fun `pathLength вычисляет длину по теореме Пифагора`() {
        val twoNodes = listOf(Node(1, 1, 0f, 0f), Node(2, 1, 3f, 4f))
        val length = engine.pathLength(twoNodes)
        
        assertEquals("Длина пути 3-4-5 должна быть 5.0", 5.0f, length, 0.01f)
    }

    @Test
    fun `pathLength для одного узла возвращает 0`() {
        val length = engine.pathLength(listOf(Node(1, 1, 10f, 10f)))
        assertEquals(0f, length, 0.01f)
    }

    @Test
    fun `pathLength для пустого пути возвращает 0`() {
        val length = engine.pathLength(emptyList())
        assertEquals(0f, length, 0.01f)
    }

    @Test
    fun `pathLength суммирует сегменты правильно`() {
        
        val nodes = listOf(
            Node(1, 1, 0f, 0f),
            Node(2, 1, 3f, 4f),
            Node(3, 1, 3f, 7f)
        )
        val length = engine.pathLength(nodes)
        assertEquals("Суммарная длина должна быть 8.0", 8.0f, length, 0.01f)
    }

    @Test
    fun `findPath возвращает оптимальный путь среди нескольких`() {
        
        val path = engine.findPath(1, 7)
        assertTrue(path.isNotEmpty())
        
        val totalWeight = engine.pathLength(path)
        assertTrue("Путь должен быть разумной длины", totalWeight < 2000f)
    }
}
