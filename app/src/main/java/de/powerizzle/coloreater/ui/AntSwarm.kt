package de.powerizzle.coloreater.ui

import android.graphics.Canvas
import android.graphics.Paint
import de.powerizzle.coloreater.game.Removal
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.random.Random

/**
 * Replays [Removal]s as ants: each ant walks from its nest (the slot) to a pixel, picks it up and
 * carries it back. Pick-ups happen in the same order the game logic removed the pixels, so an ant
 * never grabs a pixel that is still hidden behind another one.
 */
class AntSwarm {
    interface Geometry {
        fun pixelX(pixel: Int): Float
        fun pixelY(pixel: Int): Float
        fun nestX(slot: Int): Float
        fun nestY(slot: Int): Float
        /** Walking speed in px per second. */
        val antSpeed: Float
        val nestWidth: Float
    }

    private class Ant(
        val removal: Removal,
        val nestX: Float, val nestY: Float,
        val pixelX: Float, val pixelY: Float,
        val depart: Double, val pickup: Double, val arrive: Double,
        /** Sideways bend of the path, so ants don't walk in straight lines. */
        val bend: Float,
        val stride: Float,
    ) {
        var carrying = false
    }

    private val waiting = ArrayDeque<Removal>()
    private val ants = ArrayList<Ant>()
    private var lastPickup = 0.0
    private val random = Random(42)

    private val body = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = ANT_COLOR }
    private val legs = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ANT_COLOR
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val cargo = Paint()
    private val cargoEdge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x66000000
        style = Paint.Style.STROKE
    }

    val isIdle get() = waiting.isEmpty() && ants.isEmpty()

    fun add(removals: List<Removal>) {
        waiting.addAll(removals)
    }

    fun update(now: Double, geometry: Geometry, onPickup: (Removal) -> Unit) {
        spawn(now, geometry)
        for (ant in ants) {
            if (!ant.carrying && now >= ant.pickup) {
                ant.carrying = true
                onPickup(ant.removal)
            }
        }
        ants.removeAll { now >= it.arrive }
    }

    private fun spawn(now: Double, geometry: Geometry) {
        lastPickup = max(lastPickup, now - 1.0)
        while (waiting.isNotEmpty()) {
            val removal = waiting.first()
            val nestX = geometry.nestX(removal.slot) + (random.nextFloat() - 0.5f) * geometry.nestWidth * 0.6f
            val nestY = geometry.nestY(removal.slot)
            val pixelX = geometry.pixelX(removal.pixel)
            val pixelY = geometry.pixelY(removal.pixel)
            val travel = hypot(pixelX - nestX, pixelY - nestY) / geometry.antSpeed
            // The more pixels are waiting, the more ants are sent out at once.
            val rate = min(MAX_RATE, BASE_RATE + RATE_PER_WAITING * waiting.size)
            val pickup = max(now + travel, lastPickup + 1.0 / rate)
            val depart = pickup - travel
            if (depart > now + FRAME) break
            waiting.removeFirst()
            lastPickup = pickup
            val bend = (random.nextFloat() - 0.5f) * 0.25f * travel.toFloat() * geometry.antSpeed
            ants += Ant(
                removal, nestX, nestY, pixelX, pixelY, depart, pickup, pickup + travel, bend,
                stride = 18f + random.nextFloat() * 6f,
            )
        }
    }

    fun draw(canvas: Canvas, now: Double, antLength: Float, cargoSize: Float, palette: IntArray) {
        legs.strokeWidth = antLength * 0.06f
        cargoEdge.strokeWidth = max(1f, cargoSize * 0.08f)
        for (ant in ants) {
            if (now < ant.depart) continue
            val (x, y) = position(ant, now)
            val (aheadX, aheadY) = position(ant, now + 0.02)
            val angle = atan2(aheadY - y, aheadX - x)
            val cargoColor = if (ant.carrying) palette[ant.removal.volume.color] else null
            drawAnt(canvas, x, y, angle, antLength, (now * ant.stride).toFloat(), cargoColor, cargoSize)
        }
    }

    private fun position(ant: Ant, time: Double): Pair<Float, Float> {
        val outbound = time < ant.pickup
        val progress = if (outbound) {
            ((time - ant.depart) / (ant.pickup - ant.depart)).coerceIn(0.0, 1.0)
        } else {
            1.0 - ((time - ant.pickup) / (ant.arrive - ant.pickup)).coerceIn(0.0, 1.0)
        }.toFloat()
        val dx = ant.pixelX - ant.nestX
        val dy = ant.pixelY - ant.nestY
        val length = max(1f, hypot(dx, dy))
        // Walk out on one side of the straight line and back on the other.
        val offset = sin(progress * PI).toFloat() * ant.bend * (if (outbound) 1f else -1f)
        return (ant.nestX + dx * progress - dy / length * offset) to
            (ant.nestY + dy * progress + dx / length * offset)
    }

    /** Top-down ant facing [angle]; legs swing in the typical alternating tripod gait. */
    private fun drawAnt(
        canvas: Canvas, x: Float, y: Float, angle: Float, l: Float, phase: Float,
        cargoColor: Int?, cargoSize: Float,
    ) {
        canvas.save()
        canvas.translate(x, y)
        canvas.rotate(Math.toDegrees(angle.toDouble()).toFloat())

        val swing = sin(phase) * l * 0.12f
        for (i in 0..2) {
            val rootX = l * (0.08f - 0.08f * i)
            val reach = l * (0.22f - 0.22f * i)
            for (side in intArrayOf(-1, 1)) {
                val sway = if ((i + (side + 1) / 2) % 2 == 0) swing else -swing
                canvas.drawLine(rootX, 0f, rootX + reach + sway, side * l * 0.34f, legs)
            }
        }
        canvas.drawLine(l * 0.3f, 0f, l * 0.48f, -l * 0.16f, legs)
        canvas.drawLine(l * 0.3f, 0f, l * 0.48f, l * 0.16f, legs)

        canvas.drawOval(-l * 0.5f, -l * 0.15f, -l * 0.1f, l * 0.15f, body)
        canvas.drawOval(-l * 0.12f, -l * 0.08f, l * 0.14f, l * 0.08f, body)
        canvas.drawOval(l * 0.12f, -l * 0.11f, l * 0.34f, l * 0.11f, body)

        if (cargoColor != null) {
            cargo.color = cargoColor
            val left = l * 0.36f
            val half = cargoSize / 2
            canvas.drawRect(left, -half, left + cargoSize, half, cargo)
            canvas.drawRect(left, -half, left + cargoSize, half, cargoEdge)
        }
        canvas.restore()
    }

    private companion object {
        const val ANT_COLOR = 0xFF3A2418.toInt()
        const val BASE_RATE = 8.0
        const val RATE_PER_WAITING = 2.5
        const val MAX_RATE = 160.0
        const val FRAME = 1.0 / 60
    }
}
