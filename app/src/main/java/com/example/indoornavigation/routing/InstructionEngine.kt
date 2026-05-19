package com.example.indoornavigation.routing

import android.content.Context
import com.example.indoornavigation.R
import com.example.indoornavigation.data.model.Node
import com.example.indoornavigation.data.model.Step
import kotlin.math.atan2
import kotlin.math.sqrt

class InstructionEngine {

    fun buildSteps(path: List<Node>, context: Context, startRoomName: String = "", endRoomName: String = ""): List<Step> {
        if (path.isEmpty()) return emptyList()
        val steps = mutableListOf<Step>()

        // 1. START Step
        val first = path.first()
        if (first.id == -100) {
            val msg = if (startRoomName.isNotEmpty()) {
                context.getString(R.string.instruction_start_room, startRoomName)
            } else {
                context.getString(R.string.instruction_start_room_fallback)
            }
            steps.add(Step("START", msg))
        } else {
            steps.add(Step("START", context.getString(R.string.instruction_start, first.id)))
        }

        var accumulatedStraight = 0f
        var lastFloorChange = false

        for (i in 1 until path.size) {
            val prev = path[i - 1]
            val curr = path[i]

            // If we change floors
            if (prev.floorId != curr.floorId) {
                // First, write any accumulated straight distance on the previous floor
                if (accumulatedStraight > 5f) {
                    steps.add(Step("STRAIGHT", context.getString(R.string.instruction_straight)))
                    accumulatedStraight = 0f
                }

                val direction = if (curr.floorId > prev.floorId) {
                    context.getString(R.string.instruction_floor_up)
                } else {
                    context.getString(R.string.instruction_floor_down)
                }
                val via = when (curr.type) {
                    "elevator" -> context.getString(R.string.instruction_via_elevator)
                    "escalator" -> context.getString(R.string.instruction_via_escalator)
                    else -> context.getString(R.string.instruction_via_stairs)
                }
                steps.add(Step("FLOOR", context.getString(R.string.instruction_floor, direction, curr.floorId, via)))
                lastFloorChange = true
                continue
            }

            // Calculate distance of this segment
            val dist = sqrt(
                ((curr.x - prev.x) * (curr.x - prev.x) +
                        (curr.y - prev.y) * (curr.y - prev.y)).toDouble()
            ).toFloat()

            // If it's the virtual start transition (exiting the room)
            if (prev.id == -100) {
                if (dist > 1f) {
                    steps.add(Step("STRAIGHT", context.getString(R.string.instruction_exit_room)))
                }
                continue
            }

            // If it's the virtual end transition (entering the room)
            if (curr.id == -200) {
                // Output final accumulated straight before entering the room
                if (accumulatedStraight > 5f) {
                    steps.add(Step("STRAIGHT", context.getString(R.string.instruction_straight)))
                    accumulatedStraight = 0f
                }
                if (dist > 1f) {
                    steps.add(Step("STRAIGHT", context.getString(R.string.instruction_enter_room)))
                }
                continue
            }

            // Check if there is a turn angle
            // ONLY if:
            // 1. We didn't just change floors (to avoid weird stair angles)
            // 2. The previous-to-previous node is on the same floor
            // 3. The previous-to-previous node is NOT the virtual start node (to avoid weird exit room angles)
            if (i >= 2 && !lastFloorChange && path[i - 2].floorId == prev.floorId && path[i - 2].id != -100) {
                val angle = calcAngle(path[i - 2], prev, curr)
                if (angle < -35 || angle > 35) {
                    // We have a turn! First, output any accumulated straight distance
                    if (accumulatedStraight > 5f) {
                        steps.add(Step("STRAIGHT", context.getString(R.string.instruction_straight)))
                        accumulatedStraight = 0f
                    }
                    // Add the turn step
                    if (angle < -35) {
                        steps.add(Step("TURN_LEFT", context.getString(R.string.instruction_turn_left)))
                    } else {
                        steps.add(Step("TURN_RIGHT", context.getString(R.string.instruction_turn_right)))
                    }
                }
            }

            accumulatedStraight += dist
            lastFloorChange = false
        }

        // Add remaining straight distance if any
        if (accumulatedStraight > 5f) {
            steps.add(Step("STRAIGHT", context.getString(R.string.instruction_straight)))
        }

        // 3. ARRIVE Step
        val last = path.last()
        if (last.id == -200) {
            val msg = if (endRoomName.isNotEmpty()) {
                context.getString(R.string.instruction_arrive_room, endRoomName)
            } else {
                context.getString(R.string.instruction_arrive_room_fallback)
            }
            steps.add(Step("ARRIVE", msg))
        } else {
            steps.add(Step("ARRIVE", context.getString(R.string.instruction_arrive)))
        }

        return steps
    }

    private fun calcAngle(a: Node, b: Node, c: Node): Double {
        val ax = (b.x - a.x).toDouble(); val ay = (b.y - a.y).toDouble()
        val bx = (c.x - b.x).toDouble(); val by = (c.y - b.y).toDouble()
        return Math.toDegrees(atan2(ax * by - ay * bx, ax * bx + ay * by))
    }
}