package de.powerizzle.coloreater.game

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The shipped levels in assets must match what the generator produces.
 *
 * After changing the generator or the level curve, regenerate them with
 * `WRITE_LEVELS=1 ./gradlew testStandardDebugUnitTest --tests '*LevelAssetsTest*'`.
 */
class LevelAssetsTest {
    private val assets = File("src/main/assets")

    @Test
    fun shippedLevelsMatchGenerator() {
        val write = System.getenv("WRITE_LEVELS") == "1"
        for (difficulty in Difficulty.entries) for (number in 1..Levels.SHIPPED) {
            val file = File(assets, LevelCodec.assetPath(difficulty, number))
            val config = Levels.config(number, difficulty)
            if (write) {
                file.parentFile!!.mkdirs()
                file.writeText(LevelCodec.encode(LevelGenerator.generate(config)))
                continue
            }
            assertTrue("$file missing, run with WRITE_LEVELS=1", file.exists())
            // Checking a few per difficulty keeps the test fast; the rest are only decoded.
            val level = LevelCodec.decode(config, file.readText())
            if (number <= 3 || number % 15 == 0) {
                assertEquals(
                    "$file is stale, run with WRITE_LEVELS=1",
                    LevelCodec.encode(LevelGenerator.generate(config)), file.readText(),
                )
            }
            assertEquals(level.picture.remaining, level.queues.flatten().sumOf { it.count })
        }
    }

    @Test
    fun codecRoundTrips() {
        val level = LevelGenerator.generate(Levels.config(6, Difficulty.HARD))
        val decoded = LevelCodec.decode(level.config, LevelCodec.encode(level))
        assertEquals(LevelCodec.encode(level), LevelCodec.encode(decoded))
        assertTrue(decoded.queues.flatten().any { it.hidden })
    }
}
