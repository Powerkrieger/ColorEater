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
    private fun names(difficulty: Difficulty, levels: IntRange) =
        levels.map { Levels.pick(it, difficulty).art.name }

    @Test
    fun picturesDoNotRepeatForALongTime() {
        for (difficulty in Difficulty.entries) {
            val firstRepeat = (1..500).first { !Levels.pick(it, difficulty).isNew }
            println("$difficulty: first repeated picture on level $firstRepeat")
            assertTrue("$difficulty repeats on level $firstRepeat", firstRepeat > 25)
            for (number in 2..200) {
                assertTrue(Levels.pick(number, difficulty).art !== Levels.pick(number - 1, difficulty).art)
            }
        }
    }

    @Test
    fun easyAndNormalCycleThroughTheirCategories() {
        for (difficulty in listOf(Difficulty.EASY, Difficulty.NORMAL)) {
            val categories = Levels.track(difficulty).categories
            val round = categories.sumOf { it.run }
            val expected = categories.flatMap { category -> List(category.run) { category } }
            for (start in listOf(1, 1 + round, 1 + 5 * round)) {
                assertEquals(expected, (start until start + round).map { Levels.pick(it, difficulty).category })
            }
        }
    }

    @Test
    fun hardBringsInHarderCategoriesAndDropsEasyOnes() {
        val categories = Levels.track(Difficulty.HARD).categories
        val order = (1..300).map { Levels.pick(it, Difficulty.HARD).category }.distinct()
        assertEquals(categories, order)
        val late = (200..300).map { Levels.pick(it, Difficulty.HARD).category }.toSet()
        assertTrue(categories.first() !in late)
        assertTrue(categories.last() in late)
    }

    @Test
    fun categoriesDropTheirEarlyPictures() {
        for (difficulty in Difficulty.entries) {
            val late = names(difficulty, 400..500).toSet()
            for (category in Levels.track(difficulty).categories) {
                if (category.arts.size > 4) assertTrue(category.arts.first().name !in late)
            }
        }
    }
}
