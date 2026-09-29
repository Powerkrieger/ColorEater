package de.powerizzle.coloreater.game

import de.powerizzle.coloreater.game.pictures.EasyPictures
import de.powerizzle.coloreater.game.pictures.HardPictures
import de.powerizzle.coloreater.game.pictures.NormalPictures
import kotlin.math.roundToInt
import kotlin.math.sqrt

enum class Difficulty(val label: String) { EASY("Easy"), NORMAL("Normal"), HARD("Hard") }

/**
 * The level sequence. Every level shows the next picture of its difficulty's [Track]: themed
 * categories that take turns, each going from easy to harder pictures.
 */
object Levels {
    /** Levels per difficulty that ship pre-generated in assets; later ones are generated on the phone. */
    const val SHIPPED = 45

    /** Most cells a scaled-up picture may have (52 x 52). */
    private const val MAX_CELLS = 2704

    fun track(difficulty: Difficulty): Track = when (difficulty) {
        Difficulty.EASY -> EasyPictures.TRACK
        Difficulty.NORMAL -> NormalPictures.TRACK
        Difficulty.HARD -> HardPictures.TRACK
    }

    fun pick(number: Int, difficulty: Difficulty): Pick = track(difficulty).pick(number)

    fun config(number: Int, difficulty: Difficulty): LevelConfig {
        val art = pick(number, difficulty).art
        // Pictures are drawn at different sizes; they are scaled up to about the same size.
        val side = when {
            number == 1 -> 16
            number <= 9 -> 32
            else -> 48
        }
        val area = art.width * art.height
        // Bigger pictures take too long to generate on the phone.
        val scale = generateSequence(maxOf(1, (side / sqrt(area.toDouble())).roundToInt())) { it - 1 }
            .first { it == 1 || area * it * it <= MAX_CELLS }
        val maxVolume = (area * scale * scale / 17).coerceIn(20, 100)
        // Grows every 9 levels.
        val stage = (number - 1) / 9
        return when (difficulty) {
            Difficulty.EASY -> LevelConfig(
                number, art, scale, maxSlots = 5, slack = 2, queues = 4,
                minVolume = 3, maxVolume = maxVolume,
                blockers = minOf(0.5, 0.25 + 0.05 * stage),
                difficulty = difficulty,
            )
            Difficulty.NORMAL -> LevelConfig(
                number, art, scale, maxSlots = 5, slack = 1, queues = 4,
                minVolume = 3, maxVolume = maxVolume,
                blockers = minOf(0.8, 0.45 + 0.1 * stage),
                mystery = if (number >= 5) 0.25 else 0.0,
                difficulty = difficulty,
            )
            Difficulty.HARD -> {
                // Most levels leave no room for mistakes; every third one gives a spare
                // slot but hides much of what is coming.
                val mysteryLevel = number >= 5 && number % 3 == 0
                LevelConfig(
                    number, art, scale, maxSlots = 5,
                    slack = if (number == 1 || mysteryLevel) 1 else 0,
                    queues = 4,
                    minVolume = 1, maxVolume = maxVolume,
                    blockers = minOf(0.85, 0.6 + 0.1 * stage),
                    mystery = if (mysteryLevel) 0.4 else 0.0,
                    difficulty = difficulty,
                )
            }
        }
    }
}
