package com.example.indoornavigation.routing

import android.content.Context
import com.example.indoornavigation.R
import com.example.indoornavigation.data.model.Node
import com.example.indoornavigation.data.model.Step
import kotlin.math.atan2
import kotlin.math.sqrt

class InstructionEngine {

    fun buildSteps(path: List<Node>, context: Context): List<Step> {
        if (path.isEmpty()) return emptyList()
        val steps = mutableListOf<Step>()
        steps.add(Step("START", context.getString(R.string.instruction_start, path.first().id)))

        for (i in 1 until path.size) {
            val prev = path[i - 1]
            val curr = path[i]

            if (prev.floorId != curr.floorId) {
                val direction = if (curr.floorId > prev.floorId) {
                    context.getString(R.string.instruction_floor_up)
                } else {
                    context.getString(R.string.instruction_floor_down)
                }
                val via = if (curr.type == "elevator") {
                    context.getString(R.string.instruction_via_elevator)
                } else {
                    context.getString(R.string.instruction_via_stairs)
                }
                steps.add(Step("FLOOR", context.getString(R.string.instruction_floor, direction, curr.floorId, via)))
                continue
            }

            if (i >= 2) {
                val angle = calcAngle(path[i - 2], prev, curr)
                when {
                    angle < -30 -> steps.add(Step("TURN_LEFT",  context.getString(R.string.instruction_turn_left)))
                    angle >  30 -> steps.add(Step("TURN_RIGHT", context.getString(R.string.instruction_turn_right)))
                }
            }

            val dist = sqrt(
                ((curr.x - prev.x) * (curr.x - prev.x) +
                        (curr.y - prev.y) * (curr.y - prev.y)).toDouble()
            ).toFloat()
            if (dist > 5f) {
                steps.add(Step("STRAIGHT", context.getString(R.string.instruction_straight, dist.toInt())))
            }
        }

        steps.add(Step("ARRIVE", context.getString(R.string.instruction_arrive)))
        return steps
    }

    private fun calcAngle(a: Node, b: Node, c: Node): Double {
        val ax = (b.x - a.x).toDouble(); val ay = (b.y - a.y).toDouble()
        val bx = (c.x - b.x).toDouble(); val by = (c.y - b.y).toDouble()
        return Math.toDegrees(atan2(ax * by - ay * bx, ax * bx + ay * by))
    }
}