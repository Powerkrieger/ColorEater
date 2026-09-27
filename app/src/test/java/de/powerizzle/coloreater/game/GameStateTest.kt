package de.powerizzle.coloreater.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GameStateTest {
    // R R R
    // R B R
    // R R R   -> blue is only reachable after the red around it is gone
    private val art = PixelArt("test", listOf("RRR", "RBR", "RRR"))

    private fun level(queues: List<List<Volume>>, slots: Int = 2): Level {
        val (picture, palette) = art.toPicture(1)
        val config = LevelConfig(1, art, 1, slots, 0, queues.size, 1, 10, 0.0)
        return Level(config, picture, palette, queues, slots)
    }

    @Test
    fun onlyExposedPixelsAreEaten() {
        val (picture, _) = art.toPicture(1)
        val red = 0
        val blue = 1
        assertEquals(8, picture.exposedCount(red))
        assertFalse(picture.hasExposed(blue))
        picture.remove(picture.nextExposed(red)) // bottom-left corner
        assertFalse(picture.hasExposed(blue))
        picture.remove(picture.nextExposed(red)) // bottom middle, next to blue
        assertTrue(picture.hasExposed(blue))
    }

    @Test
    fun buriedColorWaitsInSlotUntilExposed() {
        val state = GameState(level(listOf(listOf(Volume(1, 1)), listOf(Volume(0, 8)))))
        assertTrue(state.pick(0).isEmpty())
        assertEquals(Status.PLAYING, state.status)
        val removals = state.pick(1)
        assertEquals(9, removals.size)
        assertEquals(Status.WON, state.status)
    }

    @Test
    fun fullSlotsThatCannotEatLose() {
        val state = GameState(level(listOf(listOf(Volume(1, 1)), listOf(Volume(1, 1)), listOf(Volume(0, 8))), slots = 2))
        state.pick(0)
        state.pick(1)
        assertEquals(Status.LOST, state.status)
    }

    @Test
    fun solverFindsWinningOrder() {
        val state = GameState(level(listOf(listOf(Volume(1, 1)), listOf(Volume(0, 4), Volume(0, 4))), slots = 1))
        val solution = Solver.solve(state)
        assertEquals(Outcome.SOLVED, solution.outcome)
        assertEquals(listOf(1, 1, 0), solution.moves)
    }
}
