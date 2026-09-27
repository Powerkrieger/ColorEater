package de.powerizzle.coloreater.game

import java.util.BitSet

/**
 * The pixel image that gets eaten.
 *
 * Cells are stored row by row starting with the bottom row, so a lower index means a pixel
 * closer to the nests below the picture. A pixel is "exposed" when it touches the border of the
 * image or an empty cell; only exposed pixels can be carried away. Among the exposed pixels of a
 * color, the one with the lowest index is always taken first, which keeps the game deterministic.
 */
class Picture private constructor(
    val width: Int,
    val height: Int,
    val colorCount: Int,
    private val cells: IntArray,
    private val exposed: Array<BitSet>,
    private val remainingByColor: IntArray,
    remaining: Int,
) {
    var remaining: Int = remaining
        private set

    val size: Int get() = cells.size

    fun isEmpty() = remaining == 0

    fun colorAt(index: Int) = cells[index]

    fun column(index: Int) = index % width

    /** Screen row, counted from the top. */
    fun row(index: Int) = height - 1 - index / width

    fun remainingOf(color: Int) = remainingByColor[color]

    fun hasExposed(color: Int) = !exposed[color].isEmpty

    fun exposedCount(color: Int) = exposed[color].cardinality()

    /** Index of the next pixel of [color] that can be taken, or -1 if none is reachable. */
    fun nextExposed(color: Int): Int = exposed[color].nextSetBit(0)

    fun remove(index: Int) {
        val color = cells[index]
        require(color != EMPTY) { "Cell $index is already empty" }
        cells[index] = EMPTY
        exposed[color].clear(index)
        remainingByColor[color]--
        remaining--
        forEachNeighbor(index) { neighbor ->
            val neighborColor = cells[neighbor]
            if (neighborColor != EMPTY) exposed[neighborColor].set(neighbor)
        }
    }

    fun copy() = Picture(
        width, height, colorCount,
        cells.copyOf(),
        Array(colorCount) { exposed[it].clone() as BitSet },
        remainingByColor.copyOf(),
        remaining,
    )

    private inline fun forEachNeighbor(index: Int, action: (Int) -> Unit) {
        val x = index % width
        if (x > 0) action(index - 1)
        if (x < width - 1) action(index + 1)
        if (index >= width) action(index - width)
        if (index + width < cells.size) action(index + width)
    }

    companion object {
        const val EMPTY = -1

        /** Builds a picture from color indices given row by row from the top ([EMPTY] = no pixel). */
        fun fromRows(width: Int, height: Int, colorCount: Int, topDown: IntArray): Picture {
            require(topDown.size == width * height)
            val cells = IntArray(width * height) { index ->
                topDown[(height - 1 - index / width) * width + index % width]
            }
            val exposed = Array(colorCount) { BitSet(cells.size) }
            val remainingByColor = IntArray(colorCount)
            for (index in cells.indices) {
                val color = cells[index]
                if (color == EMPTY) continue
                remainingByColor[color]++
                val x = index % width
                val touchesOutside = x == 0 || x == width - 1 ||
                    index < width || index + width >= cells.size ||
                    cells[index - 1] == EMPTY || cells[index + 1] == EMPTY ||
                    cells[index - width] == EMPTY || cells[index + width] == EMPTY
                if (touchesOutside) exposed[color].set(index)
            }
            return Picture(width, height, colorCount, cells, exposed, remainingByColor, remainingByColor.sum())
        }
    }
}
