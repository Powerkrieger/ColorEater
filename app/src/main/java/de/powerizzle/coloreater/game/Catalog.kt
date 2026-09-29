package de.powerizzle.coloreater.game

/**
 * Pictures of one theme, ordered from easy to hard. Every time the category comes around it shows
 * its next [run] pictures. Once all of them have been shown, only the hardest few keep coming
 * back, so the early ones drop out of rotation.
 */
class Category(val name: String, val run: Int, val arts: List<PixelArt>) {
    init {
        require(arts.size >= run) { "$name: fewer pictures than one run" }
    }

    /** The picture shown the [k]th time (from 0) this category is played. */
    fun art(k: Int): PixelArt {
        if (k < arts.size) return arts[k]
        val keep = minOf(arts.size, maxOf(KEEP, run + 1))
        return arts[arts.size - keep + (k - arts.size) % keep]
    }

    private companion object {
        /** Pictures that stay in rotation once a category has shown everything. */
        const val KEEP = 4
    }
}

/** Where a level's picture comes from; [isNew] if it has not been shown before. */
class Pick(val category: Category, val art: PixelArt, val isNew: Boolean)

/**
 * The pictures of one difficulty, played in rounds: in every round each active category shows
 * its run. Without a [window], all categories are active in every round. With one, the round
 * number decides: every round brings in the next of [categories], and once [window] of them are
 * active the easiest one leaves, until only the hardest ones are left.
 */
class Track(val categories: List<Category>, private val window: Int? = null) {
    private val picks = ArrayList<Pick>()
    private val played = IntArray(categories.size)
    private var round = 0

    init {
        require(window == null || window in 1..categories.size)
    }

    private fun active(round: Int): IntRange {
        if (window == null) return categories.indices
        val newest = minOf(round, categories.lastIndex)
        return maxOf(0, newest - window + 1)..newest
    }

    /** The picture of level [number] (from 1). */
    @Synchronized
    fun pick(number: Int): Pick {
        while (picks.size < number) {
            for (c in active(round)) {
                val category = categories[c]
                repeat(category.run) {
                    val k = played[c]++
                    picks += Pick(category, category.art(k), k < category.arts.size)
                }
            }
            round++
        }
        return picks[number - 1]
    }
}
