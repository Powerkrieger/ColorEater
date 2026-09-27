package de.powerizzle.coloreater.game

/**
 * Text format for pre-generated levels (see docs/DESIGN.md). The picture is not stored, it
 * follows from the level config:
 *
 * ```
 * slots 3
 * queue 0:12 1:40? 2:5
 * queue ...
 * ```
 * Each volume is `color:count`, with a trailing `?` if it is hidden.
 */
object LevelCodec {
    fun encode(level: Level): String = buildString {
        appendLine("slots ${level.slots}")
        for (queue in level.queues) {
            append("queue")
            for (volume in queue) append(" ${volume.color}:${volume.count}${if (volume.hidden) "?" else ""}")
            appendLine()
        }
    }

    fun decode(config: LevelConfig, text: String): Level {
        val (picture, palette) = config.art.toPicture(config.scale)
        val lines = text.lines().filter { it.isNotBlank() }
        val slots = lines.first().removePrefix("slots ").trim().toInt()
        val queues = lines.drop(1).map { line ->
            line.removePrefix("queue").trim().split(' ').filter { it.isNotEmpty() }.map { token ->
                val hidden = token.endsWith('?')
                val (color, count) = token.removeSuffix("?").split(':').map(String::toInt)
                Volume(color, count, hidden)
            }
        }
        return Level(config, picture, palette, queues, slots)
    }

    fun assetPath(difficulty: Difficulty, number: Int) =
        "levels/${difficulty.name.lowercase()}/${number.toString().padStart(3, '0')}.txt"
}
