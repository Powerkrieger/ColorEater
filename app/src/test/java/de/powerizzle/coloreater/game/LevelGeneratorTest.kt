package de.powerizzle.coloreater.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LevelGeneratorTest {
    private fun greedy(level: Level): Status {
        val state = GameState(level)
        while (state.status == Status.PLAYING) {
            state.pick(state.queues.indices.filter { state.canPick(it) }.maxBy { state.copy().pick(it).size })
        }
        return state.status
    }

    @Test
    fun generatedLevelsAreWinnableAndUseEveryPixel() {
        for (difficulty in Difficulty.entries) for (number in 1..27) {
            val start = System.nanoTime()
            val level = LevelGenerator.generate(Levels.config(number, difficulty))
            val millis = (System.nanoTime() - start) / 1_000_000
            val volumes = level.queues.flatten()
            assertEquals(level.picture.remaining, volumes.sumOf { it.count })
            assertTrue(volumes.all { it.count in 1..100 })
            val solution = Solver.solve(GameState(level))
            assertEquals("$difficulty level $number", Outcome.SOLVED, solution.outcome)
            // Informational: the generator's search is limited, so now and then a level gets a
            // slot more than strictly needed.
            val oneLess = Solver.solve(GameState(level, level.slots - 1)).outcome
            val hidden = volumes.count { it.hidden }
            println(
                "$difficulty $number ${level.config.art.name}: ${level.picture.remaining} px, " +
                    "${volumes.size} volumes ($hidden hidden), ${level.slots} slots, " +
                    "slack ${level.config.slack}, one less: $oneLess, greedy ${greedy(level)}, ${millis}ms"
            )
        }
    }
}

class LevelsTest {
    @Test
    fun picturesAreIntroducedSlowlyAndNotRepeatedBackToBack() {
        val firstSeen = Levels.ARTS.indices.map { art -> (1..40).first { Levels.artIndex(it) == art } }
        assertEquals(listOf(1, 2, 4, 7, 10, 13, 16, 19, 22), firstSeen)
        for (number in 2..40) assertTrue(Levels.artIndex(number) != Levels.artIndex(number - 1))
    }
}
