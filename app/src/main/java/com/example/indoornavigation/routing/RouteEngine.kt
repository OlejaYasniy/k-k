package com.example.indoornavigation.routing

import com.example.indoornavigation.data.model.CrossFloorEdge
import com.example.indoornavigation.data.model.Edge
import com.example.indoornavigation.data.model.Node
import kotlin.math.hypot

class RouteEngine(
    private val nodes: List<Node>,
    private val edges: List<Edge>,
    private val crossFloorEdges: List<CrossFloorEdge> = emptyList()
) {
    private val nodesById = nodes.associateBy { it.id }

    private val adjacency: Map<Int, List<Pair<Int, Float>>> by lazy {
        val map = mutableMapOf<Int, MutableList<Pair<Int, Float>>>()
        for (e in edges) {
            map.getOrPut(e.from) { mutableListOf() }.add(e.to   to e.weight)
            map.getOrPut(e.to)   { mutableListOf() }.add(e.from to e.weight)
        }
        for (e in crossFloorEdges) {
            map.getOrPut(e.fromNodeId) { mutableListOf() }.add(e.toNodeId   to e.weight)
            map.getOrPut(e.toNodeId)   { mutableListOf() }.add(e.fromNodeId to e.weight)
        }
        map
    }

    fun nearestNode(x: Float, y: Float): Node? =
        nodes.minByOrNull { hypot((it.x - x).toDouble(), (it.y - y).toDouble()) }

    fun findPath(fromNodeId: Int, toNodeId: Int): List<Node> {
        val goal     = nodesById[toNodeId] ?: return emptyList()
        val gScore   = mutableMapOf<Int, Float>().withDefault { Float.MAX_VALUE }
        val fScore   = mutableMapOf<Int, Float>().withDefault { Float.MAX_VALUE }
        val cameFrom = mutableMapOf<Int, Int>()
        val openSet  = mutableSetOf(fromNodeId)

        gScore[fromNodeId] = 0f
        fScore[fromNodeId] = heuristic(nodesById[fromNodeId] ?: return emptyList(), goal)

        while (openSet.isNotEmpty()) {
            val current = openSet.minByOrNull { fScore.getValue(it) }!!
            if (current == toNodeId) return reconstructPath(cameFrom, current)
            openSet.remove(current)
            for ((neighbor, weight) in adjacency[current].orEmpty()) {
                val tentativeG = gScore.getValue(current) + weight
                if (tentativeG < gScore.getValue(neighbor)) {
                    cameFrom[neighbor] = current
                    gScore[neighbor]   = tentativeG
                    fScore[neighbor]   = tentativeG +
                            (nodesById[neighbor]?.let { heuristic(it, goal) } ?: 0f)
                    openSet.add(neighbor)
                }
            }
        }
        return emptyList()
    }

    fun pathLength(path: List<Node>): Float {
        if (path.size < 2) return 0f
        var total = 0f
        for (i in 0 until path.size - 1) {
            val a = path[i]; val b = path[i + 1]
            total += hypot((b.x - a.x).toDouble(), (b.y - a.y).toDouble()).toFloat()
        }
        return total
    }

    private fun heuristic(a: Node, b: Node) =
        hypot((a.x - b.x).toDouble(), (a.y - b.y).toDouble()).toFloat()

    private fun reconstructPath(cameFrom: Map<Int, Int>, end: Int): List<Node> {
        val path = mutableListOf<Int>()
        var cur: Int? = end
        while (cur != null) { path.add(cur); cur = cameFrom[cur] }
        return path.reversed().mapNotNull { nodesById[it] }
    }
}