package de.powerizzle.coloreater.game

/**
 * A hand-drawn picture. Each character is one pixel, '.' is transparent and every other
 * character is looked up in [PixelArt.PALETTE].
 */
class PixelArt(val name: String, val rows: List<String>) {
    val width = rows.first().length
    val height = rows.size

    init {
        require(rows.all { it.length == width }) { "$name: rows differ in length" }
        require(rows.all { row -> row.all { it == '.' || it in PALETTE } }) { "$name: unknown color" }
    }

    /** Color indices are assigned in order of first appearance; returns the picture and its ARGB palette. */
    fun toPicture(scale: Int): Pair<Picture, IntArray> {
        val chars = rows.joinToString("").filter { it != '.' }.toSet().toList()
        val w = width * scale
        val h = height * scale
        val cells = IntArray(w * h) { i ->
            val c = rows[i / w / scale][i % w / scale]
            if (c == '.') Picture.EMPTY else chars.indexOf(c)
        }
        return Picture.fromRows(w, h, chars.size, cells) to IntArray(chars.size) { PALETTE.getValue(chars[it]) }
    }

    companion object {
        val PALETTE = mapOf(
            'K' to 0xFF2B2438.toInt(), // outline
            'R' to 0xFFE0453A.toInt(), // red
            'D' to 0xFF962D2D.toInt(), // dark red
            'W' to 0xFFF7F4EE.toInt(), // white
            'S' to 0xFFEED6A6.toInt(), // stem
            'B' to 0xFF7B4A2B.toInt(), // brown
            'G' to 0xFF4CAF50.toInt(), // green
            'Y' to 0xFFF6CF3F.toInt(), // yellow
            'O' to 0xFFF08A24.toInt(), // orange
            'Q' to 0xFFC0621C.toInt(), // dark orange
            'T' to 0xFFD9B77E.toInt(), // tan
            'C' to 0xFF8A9499.toInt(), // gray
            'L' to 0xFF4FA3E0.toInt(), // blue
            'P' to 0xFFF28DB2.toInt(), // pink
            'V' to 0xFF8E5CC7.toInt(), // purple
            'N' to 0xFF2F4A8A.toInt(), // navy
            'E' to 0xFF2E7D4F.toInt(), // dark green
            'M' to 0xFFA8DC6E.toInt(), // light green
            'A' to 0xFFA0E3F0.toInt(), // ice blue
            'F' to 0xFFF0C8A0.toInt(), // skin
            'H' to 0xFF4A3020.toInt(), // dark brown
        )
    }
}

class LevelConfig(
    val number: Int,
    val art: PixelArt,
    /** Every art pixel becomes a scale x scale block of game pixels. */
    val scale: Int,
    /** Most slots a level may get; the generator's intended solution fits into them. */
    val maxSlots: Int,
    /** Extra slots on top of the fewest a level can be won with. 0 = no room for mistakes. */
    val slack: Int,
    val queues: Int,
    val minVolume: Int,
    val maxVolume: Int,
    /** Chance that the next volume of the intended solution is one that has to wait in a slot. */
    val blockers: Double,
    /** Share of queued volumes that stay hidden until they reach the front. */
    val mystery: Double = 0.0,
    val difficulty: Difficulty = Difficulty.NORMAL,
) {
    val seed: Long get() = number * 7919L + difficulty.ordinal * 1_000_003L + 17
}

class Level(
    val config: LevelConfig,
    val picture: Picture,
    val palette: IntArray,
    val queues: List<List<Volume>>,
    val slots: Int,
)
