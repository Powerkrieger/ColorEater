package de.powerizzle.coloreater.ui

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import de.powerizzle.coloreater.game.PixelArt
import de.powerizzle.coloreater.game.Removal
import de.powerizzle.coloreater.game.Volume
import kotlin.math.PI
import kotlin.random.Random

/**
 * The title screen animation: a pixel flower grows out of a soil mound, blooms for a moment, and then
 * ants from a random direction carry away everything but the soil. Then the next flower grows, five of them in random order.
 */
class FlowerIntro {
    private enum class Phase { GROWING, BLOOMING, EATING, RESTING }

    private class Flower(art: PixelArt) {
        val chars = art.rows.joinToString("").filter { it != '.' }.toSet().toList()
        val palette = IntArray(chars.size) { PixelArt.PALETTE.getValue(chars[it]) }
        val colors = IntArray(SIZE * SIZE) { i ->
            val c = art.rows[i / SIZE][i % SIZE]
            if (c == '.') -1 else chars.indexOf(c)
        }
        val soil = colors.indices.filter { colors[it] >= 0 && it / SIZE >= SIZE - SOIL_ROWS }
    }

    private val flowers = FLOWERS.map(::Flower)
    private val random = Random(System.nanoTime())
    private val bag = ArrayList<Int>()
    private val bitmap = Bitmap.createBitmap(SIZE, SIZE, Bitmap.Config.ARGB_8888)
    private val pixels = Paint().apply { isFilterBitmap = false }

    private lateinit var flower: Flower
    private var swarm = AntSwarm()
    private var phase = Phase.GROWING
    private var phaseStart = -1.0

    /** Direction the ants come from, in radians (0 = from the right); new for every flower. */
    var antDirection = 0.0
        private set

    /** Pixels in the order they grow: outwards from the soil, a little shuffled. */
    private var order = IntArray(0)
    private var grown = 0

    init {
        flower = flowers[nextFlower()]
        startFlower()
    }

    private fun nextFlower(): Int {
        if (bag.isEmpty()) {
            bag += flowers.indices.shuffled(random)
            // Never the same flower twice in a row across a refill.
            if (::flower.isInitialized && flowers[bag.last()] === flower) bag.add(0, bag.removeAt(bag.lastIndex))
        }
        return bag.removeAt(bag.lastIndex)
    }

    private fun startFlower() {
        bitmap.eraseColor(Color.TRANSPARENT)
        for (pixel in flower.soil) show(pixel)
        order = growthOrder(flower)
        grown = 0
        swarm = AntSwarm()
        phase = Phase.GROWING
    }

    private fun growthOrder(flower: Flower): IntArray {
        val seen = BooleanArray(SIZE * SIZE)
        var layer = flower.soil.onEach { seen[it] = true }
        val result = ArrayList<Int>()
        while (layer.isNotEmpty()) {
            val next = ArrayList<Int>()
            for (pixel in layer) {
                val x = pixel % SIZE
                for (neighbor in intArrayOf(
                    if (x > 0) pixel - 1 else -1, if (x < SIZE - 1) pixel + 1 else -1,
                    pixel - SIZE, pixel + SIZE,
                )) {
                    if (neighbor !in seen.indices || seen[neighbor] || flower.colors[neighbor] < 0) continue
                    seen[neighbor] = true
                    next += neighbor
                }
            }
            next.shuffle(random)
            result += next
            layer = next
        }
        return result.toIntArray()
    }

    private fun show(pixel: Int) = bitmap.setPixel(pixel % SIZE, pixel / SIZE, flower.palette[flower.colors[pixel]])

    fun update(now: Double, geometry: AntSwarm.Geometry) {
        if (phaseStart < 0) phaseStart = now
        val elapsed = now - phaseStart
        when (phase) {
            Phase.GROWING -> {
                val target = minOf(order.size, (elapsed * GROW_RATE).toInt())
                while (grown < target) show(order[grown++])
                if (grown == order.size) enter(Phase.BLOOMING, now)
            }
            Phase.BLOOMING -> if (elapsed > BLOOM_TIME) {
                antDirection = random.nextDouble(2 * PI)
                // Outermost pixels first, the way they grew but backwards.
                swarm.add(order.reversed().map { Removal(0, it, Volume(flower.colors[it], 1)) })
                enter(Phase.EATING, now)
            }
            Phase.EATING -> {
                swarm.update(now, geometry) { bitmap.setPixel(it.pixel % SIZE, it.pixel / SIZE, Color.TRANSPARENT) }
                if (swarm.isIdle) enter(Phase.RESTING, now)
            }
            Phase.RESTING -> if (elapsed > REST_TIME) {
                flower = flowers[nextFlower()]
                startFlower()
                enter(Phase.GROWING, now)
            }
        }
    }

    private fun enter(next: Phase, now: Double) {
        phase = next
        phaseStart = now
    }

    fun draw(canvas: Canvas, rect: RectF, now: Double, antLength: Float, cargoSize: Float) {
        canvas.drawBitmap(bitmap, null, rect, pixels)
        swarm.draw(canvas, now, antLength, cargoSize, flower.palette)
    }

    companion object {
        const val SIZE = 16
        private const val SOIL_ROWS = 2
        private const val GROW_RATE = 40.0
        private const val BLOOM_TIME = 1.5
        private const val REST_TIME = 0.8

        private val SOIL = listOf(
            ".....BBBBBB.....",
            "...BBBBBBBBBB...",
        )
        private val STEM = listOf(
            ".......GG.......",
            "..GGG..GG.......",
            "...GGGGGG..GG...",
            ".......GGGGG....",
            ".......GG.......",
        )

        /** Every pixel must connect to the soil, since that is where the flower grows from. */
        val FLOWERS = listOf(
            PixelArt(
                "Tulip", listOf(
                    "................",
                    "................",
                    "................",
                    "....R..RR..R....",
                    "....RR.RR.RR....",
                    "....RRRRRRRR....",
                    "....RRRRRRDR....",
                    "....RRRRRRDR....",
                    ".....RRRRDR.....",
                    "......RRRR......",
                    ".......GG.......",
                    "...GG..GG..GG...",
                    "....GG.GG.GG....",
                    ".....GGGGGG.....",
                ) + SOIL
            ),
            PixelArt(
                "Sunflower", listOf(
                    "................",
                    "......Y..Y......",
                    "....Y.YYYY.Y....",
                    "....YYBBBBYY....",
                    "..YYYBBOBBBYYY..",
                    "...YYBOBBOBYY...",
                    "..YYYBBBOBBYYY..",
                    "....YYBBBBYY....",
                    "....Y.YYYY.Y....",
                ) + STEM + SOIL
            ),
            PixelArt(
                "Cornflower", listOf(
                    "................",
                    "......LLLL......",
                    "..LL..LLLL..LL..",
                    "..LLL.LLLL.LLL..",
                    "...LLLLLLLLLL...",
                    ".LLLLLYYYYLLLLL.",
                    "..LLLYYOOYYLLL..",
                    "..LLLYYOOYYLLL..",
                    ".LLLLLYYYYLLLLL.",
                    "...LLLLLLLLLL...",
                    "..LLL.LLLL.LLL..",
                    ".......GG.......",
                    ".....GGGG.......",
                    ".......GG.......",
                ) + SOIL
            ),
            PixelArt(
                "Rose", listOf(
                    "................",
                    ".....PPPPPP.....",
                    "....PPRRRRPP....",
                    "...PPRPPPPRPP...",
                    "...PRPPRRPPRP...",
                    "...PRPRPPRPRP...",
                    "...PPRPRRPRPP...",
                    "....PPRPPRPP....",
                    ".....PPPPPP.....",
                    "......GGGG......",
                ) + STEM.drop(1) + SOIL
            ),
            PixelArt(
                "Violet", listOf(
                    "................",
                    "....VV....VV....",
                    "...VVVV..VVVV...",
                    "...VVVVVVVVVV...",
                    "....VVVYYVVV....",
                    "..VVVVYOOYVVVV..",
                    ".VVVVVVYYVVVVVV.",
                    ".VVVVVVVVVVVVVV.",
                    "..VVVV.GG.VVVV..",
                ) + STEM + SOIL
            ),
        )
    }
}
