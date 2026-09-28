package de.powerizzle.coloreater.game

enum class Difficulty(val label: String) { EASY("Easy"), NORMAL("Normal"), HARD("Hard") }

/**
 * The level sequence. Pictures are rewards: a new one only shows up every few levels (see
 * [art]), the levels in between reuse pictures the player has already seen, bigger or harder.
 */
object Levels {
    /** Levels per difficulty that ship pre-generated in assets; later ones are generated on the phone. */
    const val SHIPPED = 45

    val ARTS = listOf(
        PixelArt(
            "Heart", listOf(
                "..KKKK....KKKK..",
                ".KRRRRK..KRRRRK.",
                "KRRWWRRKKRRRRRRK",
                "KRRWRRRRRRRRRRRK",
                "KRRRRRRRRRRRRRRK",
                "KRRRRRRRRRRRRRRK",
                "KRRRRRRRRRRRRRDK",
                ".KRRRRRRRRRRRDK.",
                "..KRRRRRRRRRDK..",
                "...KRRRRRRRDK...",
                "....KRRRRRDK....",
                ".....KRRRDK.....",
                "......KRDK......",
                ".......KK.......",
            )
        ),
        PixelArt(
            "Flower", listOf(
                ".....YYYYYY.....",
                "...YYYYYYYYYY...",
                "..YYYYOOOOYYYY..",
                ".YYYYOOOOOOYYYY.",
                ".YYYOOOBBOOOYYY.",
                ".YYYOOBBBBOOYYY.",
                ".YYYOOBBBBOOYYY.",
                ".YYYOOOBBOOOYYY.",
                ".YYYYOOOOOOYYYY.",
                "..YYYYOOOOYYYY..",
                "...YYYYYYYYYY...",
                ".....YYGGYY.....",
                "..GG...GG...GG..",
                "..GGGG.GG.GGGG..",
                "...GGGGGGGGGG...",
                ".......GG.......",
            )
        ),
        PixelArt(
            "Dice", listOf(
                ".KKKKKKKKKKKKKK.",
                "KWWWWWWWWWWWWWWK",
                "KWKKWWWWWWWWKKWK",
                "KWKKWWWWWWWWKKWK",
                "KWWWWWWWWWWWWWWK",
                "KWWWWWWWWWWWWWWK",
                "KWWWWWWWWWWWWWWK",
                "KWWWWWWKKWWWWWWK",
                "KWWWWWWKKWWWWWWK",
                "KWWWWWWWWWWWWWWK",
                "KWWWWWWWWWWWWWWK",
                "KWWWWWWWWWWWWWWK",
                "KWKKWWWWWWWWKKWK",
                "KWKKWWWWWWWWKKWK",
                "KWWWWWWWWWWWWWWK",
                ".KKKKKKKKKKKKKK.",
            )
        ),
        PixelArt(
            "Mushroom", listOf(
                "......KKKK......",
                "....KKRRWWKK....",
                "...KRRRRWWWRK...",
                "..KWWRRRRWWRRK..",
                ".KWWWWRRRRRRRRK.",
                ".KWWWWRRRWWRRRK.",
                "KRWWRRRRWWWWRRRK",
                "KRRRRRRRWWWWRRRK",
                "KRRWWRRRRWWRRWWK",
                "KRWWWWRRRRRRWWWK",
                ".KKKKKKKKKKKKKK.",
                "...KSSSSSSSSK...",
                "...KSSKSSKSSK...",
                "...KSSSSSSSSK...",
                "....KSSSSSSK....",
                ".....KKKKKK.....",
            )
        ),
        PixelArt(
            "Target", listOf(
                ".....KKKKKK.....",
                "...KKRRRRRRKK...",
                "..KRRWWWWWWRRK..",
                ".KRWWRRRRRRWWRK.",
                ".KRWRRWWWWRRWRK.",
                "KRWRWWRRRRWWRWRK",
                "KRWRWRRLLRRWRWRK",
                "KRWRWRLLLLRWRWRK",
                "KRWRWRLLLLRWRWRK",
                "KRWRWRRLLRRWRWRK",
                "KRWRWWRRRRWWRWRK",
                ".KRWRRWWWWRRWRK.",
                ".KRWWRRRRRRWWRK.",
                "..KRRWWWWWWRRK..",
                "...KKRRRRRRKK...",
                ".....KKKKKK.....",
            )
        ),
        PixelArt(
            "Apple", listOf(
                ".......BB.......",
                "......BB.GG.....",
                "......B.GGGG....",
                "....RRBRRGG.....",
                "..RRRRRRRRRR....",
                ".RRWRRRRRRRRR...",
                ".RWWRRRRRRRRRR..",
                "RRWRRRRRRRRRRRR.",
                "RRRRRRRRRRRRRRR.",
                "RRRRRRRRRRRRRRD.",
                "RRRRRRRRRRRRRRD.",
                ".RRRRRRRRRRRRDD.",
                ".RRRRRRRRRRRRD..",
                "..RRRRRRRRRRDD..",
                "...RRRDDRRRDD...",
                "....DDD..DDD....",
            )
        ),
        PixelArt(
            "Chess", listOf(
                "BBBBBBBBBBBBBBBB",
                "BWWKKWWKKWWKKWWB",
                "BWWKKWWKKWWKKWWB",
                "BKKWWKKWWKKWWKKB",
                "BKKWWKKWWKKWWKKB",
                "BWWKKWWKKWWKKWWB",
                "BWWKKWWKKWWKKWWB",
                "BKKWWKKWWKKWWKKB",
                "BKKWWKKWWKKWWKKB",
                "BWWKKWWKKWWKKWWB",
                "BWWKKWWKKWWKKWWB",
                "BKKWWKKWWKKWWKKB",
                "BKKWWKKWWKKWWKKB",
                "BWWKKWWKKWWKKWWB",
                "BWWKKWWKKWWKKWWB",
                "BBBBBBBBBBBBBBBB",
            )
        ),
        PixelArt(
            "Fish", listOf(
                ".......KKKK.....",
                ".....KKOOOOKK...",
                "K...KOOOWWOOOK..",
                "KK.KOOOWKWOOOOK.",
                "KOKOOOOOWWOOOOOK",
                "KOOOOYYOOOOOOOOK",
                "KOKOOOYYOOOOOOK.",
                "KK.KOOOOOOOOOK..",
                "K...KOOOOOOOK...",
                ".....KKOOOKK....",
                ".......KKK......",
            )
        ),
        PixelArt(
            "House", listOf(
                "..........CC....",
                "..........CC....",
                ".......RR.CC....",
                "......RRRRCC....",
                ".....RRRRRRR....",
                "....RRRRRRRRR...",
                "...RRRRRRRRRRR..",
                "..RRRRRRRRRRRRR.",
                ".RRRRRRRRRRRRRRR",
                "...TTTTTTTTTTT..",
                "...TLLTTTTTLLT..",
                "...TLLTTTTTLLT..",
                "...TTTTBBBTTTT..",
                "...TTTTBBBTTTT..",
                "...TTTTBBBTTTT..",
                "GGGGGGGGGGGGGGGG",
            )
        ),
    )

    /** Normal has its own pictures: many colors, with details walled in behind other colors. */
    val NORMAL_ARTS = listOf(
        PixelArt(
            "Strawberry", listOf(
                "................",
                ".......EE.......",
                "...KKKGEEGKKK...",
                "..KGGGGEEGGGGK..",
                ".KGGEGGGGGGEGGK.",
                ".KRRGGRGGRGGRRK.",
                "KRRWRRRRRRRRRRDK",
                "KRWRRYRRRRYRRRDK",
                "KRRRRRRRYRRRRDDK",
                ".KRYRRRRRRRYRDK.",
                ".KRRRRYRRRRRRDK.",
                "..KRRRRRRYRRDK..",
                "..KRRYRRRRRRDK..",
                "...KRRRRRYRDK...",
                "....KDRRRRDK....",
                ".....KKKKKK.....",
            )
        ),
        PixelArt(
            "Pumpkin", listOf(
                "........EE......",
                ".......EE.......",
                "...KKKKKKKKKK...",
                "..KOOBOOOOBOOK..",
                ".KOOBOOOOOOBOOK.",
                "KOOOKOOOOOOKOOOK",
                "KOOKYKOOOOKYKOOK",
                "KOKYYYKOOKYYYKOK",
                "KOKKKKKOOKKKKKOK",
                "KOBOOOOKKOOOOBOK",
                "KOKKKKKKKKKKKKOK",
                "KOKYYKYYYYKYYKOK",
                ".KOKYYYYYYYYKOK.",
                ".KOOKKYKKYKKOOK.",
                "..KKOKKOOKKOKK..",
                "....KKKKKKKK....",
            )
        ),
        PixelArt(
            "Frog", listOf(
                "................",
                "..KKKK....KKKK..",
                ".KWWWWK..KWWWWK.",
                ".KWKKWKKKKWKKWK.",
                ".KWKKWGGGGWKKWK.",
                "KGKWWKGGGGKWWKGK",
                "KGGKKGGGGGGKKGGK",
                "KGPGGGGGGGGGGPGK",
                "KGGKGGGGGGGGKGGK",
                "KGGGKKKKKKKKGGGK",
                ".KGGGMMMMMMGGGK.",
                ".KGEGMMMMMMGEGK.",
                "..KGGMMMMMMGGK..",
                ".KGGKKGGGGKKGGK.",
                "KGGK..KGGK..KGGK",
                "KKKK...KK...KKKK",
            )
        ),
        PixelArt(
            "Butterfly", listOf(
                "....K......K....",
                ".....K....K.....",
                ".KKK..K..K..KKK.",
                "KVVVKK.KK.KKVVVK",
                "KVPPVVKKKKVVPPVK",
                "KVPWPVVKKVVPWPVK",
                "KVPPPVVKKVVPPPVK",
                "KVVVVVLKKLVVVVVK",
                ".KVVVLLKKLLVVVK.",
                "..KKKKKKKKKKKK..",
                ".KLLLLVKKVLLLLK.",
                "KLLYLLVKKVLLYLLK",
                "KLYOYLVKKVLYOYLK",
                "KLLYLLKKKKLLYLLK",
                ".KLLLK.KK.KLLLK.",
                "..KKK......KKK..",
            )
        ),
        PixelArt(
            "Rocket", listOf(
                ".......KK.......",
                "......KRRK......",
                ".....KRRRRK.....",
                "....KKKKKKKK....",
                "....KWWWWWCK....",
                "....KWWKKWCK....",
                "....KWKALKCK....",
                "....KWKLLKCK....",
                "....KWWKKWCK....",
                "....KWWWWWCK....",
                "..KKKWWWWWCKKK..",
                ".KRRKWRRRRCKRRK.",
                ".KRRKWWWWWCKRRK.",
                ".KKKKKKKKKKKKKK.",
                "......OYYO......",
                ".......OO.......",
            )
        ),
        PixelArt(
            "Crown", listOf(
                "................",
                "..K....KK....K..",
                ".KRK..KLLK..KRK.",
                "..KK...KK...KK..",
                ".KYYK.KYYK.KYYK.",
                ".KYYYKYYYYKYYYK.",
                ".KYYYYYYYYYYYYK.",
                ".KYYYYYYYYYYYYK.",
                ".KKKKKKKKKKKKKK.",
                ".KYYYYYYYYYYYYK.",
                ".KYRWYYLWYYGWYK.",
                ".KYRRYYLLYYGGYK.",
                ".KYYYYYYYYYYYYK.",
                ".KKKKKKKKKKKKKK.",
                ".KVVVVVVVVVVVVK.",
                ".KKKKKKKKKKKKKK.",
            )
        ),
        PixelArt(
            "Clock", listOf(
                "..KKK......KKK..",
                ".KYYYK.KK.KYYYK.",
                ".KYYKKKYYKKKYYK.",
                "..KKRRRRRRRRKK..",
                ".KRRKKWWWWKKRRK.",
                ".KRKWWWCCWWWKRK.",
                "KRKWWWWKWWWWWKRK",
                "KRKWCWWKWWWWCKRK",
                "KRKWWWWRKKKWWKRK",
                "KRKWWWWWWWWWWKRK",
                ".KRKWWWWCWWWKRK.",
                ".KRRKKWWWWKKRRK.",
                "..KKRRRRRRRRKK..",
                "...KKKKKKKKKK...",
                "...KK......KK...",
                "................",
            )
        ),
        PixelArt(
            "Potion", listOf(
                "......KKKK......",
                ".....KBBBBK.....",
                ".....KBBBBK.....",
                "......KAAK......",
                "......KAAK......",
                ".....KAAWAK.....",
                "...KKAAAAAAKK...",
                "..KAAAAAAAAWAK..",
                ".KAVVVVVVVVVVAK.",
                ".KVVWVVPVVVYVVK.",
                "KAVVVVVVVWVVVVAK",
                "KAVPVVYVVVVVPVAK",
                "KAVVVWVVVPVVVVAK",
                ".KAVVVVVYVVVVAK.",
                "..KKAAAAAAAAKK..",
                "....KKKKKKKK....",
            )
        ),
        PixelArt(
            "Aquarium", listOf(
                "KKKKKKKKKKKKKKKK",
                "KAAAAAAAAAAAAAAK",
                "KLLLLLLLLLLWLLLK",
                "KLLLLLLLLLLLLWLK",
                "KLLKKKLLLLLLWLLK",
                "KLKOOOKLLLLLLLLK",
                "KKOOKWOKLLLKKKLK",
                "KOKOOOOKLLKYYYKK",
                "KKOOOOKLLKYKWYYK",
                "KLKKKKLELLKYYYKK",
                "KLLELLLELLLKKKLK",
                "KLLEELEELLLLGLLK",
                "KLELEELELLGLGLLK",
                "KTTTTYTTTTTTTTYK",
                "KTYTTTTTYTTTTTTK",
                "KKKKKKKKKKKKKKKK",
            )
        ),
    )

    /**
     * Hard has its own pictures: more colors, and colors walled in by other colors (often the
     * same color at several depths), so more volumes have to wait in slots.
     */
    val HARD_ARTS = listOf(
        PixelArt(
            "Eye", listOf(
                "................",
                "....K..K..K.....",
                ".....KKKKKK.....",
                "...KKWWWWWWKK...",
                "..KWWWLLLLWWWK..",
                ".KWWWLNNNNLWWWK.",
                "KWWWLNNKKNNLWWWK",
                "KWWWLNKWKKNLWWWK",
                "KWWWLNKKKKNLWWWK",
                "KWWWLNNKKNNLWWWK",
                ".KWWWLNNNNLWWWK.",
                "..KWWWLLLLWWWK..",
                "...KKWWWWWWKK...",
                ".....KKKKKK.....",
                "................",
                "................",
            )
        ),
        PixelArt(
            "Ladybug", listOf(
                "................",
                "....K......K....",
                ".....K....K.....",
                "......KKKK......",
                ".....KWKKWK.....",
                "...KKKKKKKKKK...",
                "K.KRWRRKKRRRDK.K",
                ".KRRKKRKKRKKRDK.",
                ".KRRKKRKKRKKRDK.",
                "KKRRRRRKKRRRRRKK",
                ".KRKKRRKKRRKKDK.",
                ".KRKKRRKKRRKKDK.",
                "KKRRRRRKKRRRRDKK",
                "..KRRKRKKRKRDK..",
                "...KRRRKKRRDK...",
                "....KKKKKKKK....",
            )
        ),
        PixelArt(
            "Watermelon", listOf(
                "................",
                "................",
                "................",
                "EEEEEEEEEEEEEEEE",
                "EMMMMMMMMMMMMMME",
                ".EWWWWWWWWWWWWE.",
                ".EWRRRRRRRRRRWE.",
                "..EWRRKRRRKRWE..",
                "..EWRRRRKRRRWE..",
                "...EWRKRRRKWE...",
                "....EWRRKRWE....",
                ".....EWRRWE.....",
                "......EWWE......",
                ".......EE.......",
                "................",
                "................",
            )
        ),
        PixelArt(
            "Donut", listOf(
                ".....KKKKKK.....",
                "...KKPPPPPPKK...",
                "..KPPYPPPLPPPK..",
                ".KPLPPPVPPPPYPK.",
                ".KPPPPKKKKPGPPK.",
                "KPPGPKTTTTKPPPPK",
                "KPPPKTK..KTKPVPK",
                "KPYPKTK..KTKPPPK",
                "KPPPPKTTTTKPPLPK",
                "KTPVPPKKKKPPYPTK",
                ".KTPPPLPPGPPPTK.",
                ".KTTPYPPPPVPTTK.",
                "..KTTTPPPPTTTK..",
                "...KKTTTTTTKK...",
                ".....KKKKKK.....",
                "................",
            )
        ),
        PixelArt(
            "Owl", listOf(
                "..K..........K..",
                "..KBK......KBK..",
                "..KBBKKKKKKBBK..",
                "..KBBBBBBBBBBK..",
                ".KBKKKKBBKKKKBK.",
                ".KKWWWWKKWWWWKK.",
                ".KWYYYWKKWYYYWK.",
                ".KWYKKWOOWKKYWK.",
                ".KWYKKWOOWKKYWK.",
                ".KKWWWWKKWWWWKK.",
                ".KBBTTTTTTTTBBK.",
                ".KBTTBTTBTTBTBK.",
                ".KBTTTTTTTTTTBK.",
                ".KBBTBTTBTTBBBK.",
                "..KBBTTTTTTBBK..",
                "BBBBBOOBBOOBBBBB",
            )
        ),
        PixelArt(
            "Chest", listOf(
                "................",
                "...R......G.....",
                "..RRR.YY.GGG.L..",
                "..KKKKKKKKKKKKK.",
                ".KBBBYBBBBBYBBBK",
                ".KBRBYBBBBBYBLBK",
                ".KYYYYYYYYYYYYYK",
                ".KBBBYBKKKBYBBBK",
                ".KBBBYKYYYKYBBBK",
                ".KBBBYKYKYKYBBBK",
                ".KBBBYKYKYKYBBBK",
                ".KBBBYKYYYKYBBBK",
                ".KBBBYBKKKBYBBBK",
                ".KYYYYYYYYYYYYYK",
                ".KKKKKKKKKKKKKKK",
                "................",
            )
        ),
        PixelArt(
            "Cake", listOf(
                ".......Y........",
                "......YOY.......",
                ".......O........",
                ".......L........",
                "......KLK.......",
                "...KKKKLKKKKK...",
                "..KPPRPPPPRPPK..",
                "..KPRYRPPRYRPK..",
                "..KPPRPPPPRPPK..",
                "..KWWWWWWWWWWK..",
                "..KTTTTTTTTTTK..",
                "..KRRRRRRRRRRK..",
                "..KTTTTTTTTTTK..",
                "..KWWWWWWWWWWK..",
                ".KCCCCCCCCCCCCK.",
                "..KKKKKKKKKKKK..",
            )
        ),
        PixelArt(
            "Doll", listOf(
                "......KKKK......",
                "....KKRRRRKK....",
                "...KRRYYYYRRK...",
                "...KRYSSSSYRK...",
                "..KRYSKSSKSYRK..",
                "..KRYPSSSSPYRK..",
                "..KRYSSRRSSYRK..",
                "..KRRYSSSSYRRK..",
                ".KRRRRYYYYRRRRK.",
                ".KRRWWWWWWWWRRK.",
                ".KRWWWRRRRWWWRK.",
                ".KRWWRYYYYRWWRK.",
                ".KRWWRYGGYRWWRK.",
                ".KRWWWRRRRWWWRK.",
                ".KRRWWWGGWWWRRK.",
                "..KKKKKKKKKKKK..",
            )
        ),
        PixelArt(
            "Snow globe", listOf(
                ".....KKKKKK.....",
                "...KKAAWAAAKK...",
                "..KAAAAAAAAAWK..",
                ".KAWAAAAKKKAAAK.",
                ".KAAEAAKKKKKAWK.",
                "KAAAEAAWKWKWAAAK",
                "KAAEEEAWWOWWAAWK",
                "KAEEEEARRRRRAAAK",
                "KEEEEEWWRKWWWAAK",
                "KAABAAWWWWWWWAWK",
                "KWABAAWWWKWWWAAK",
                ".KWWWWWWWWWWWWK.",
                "..KKKKKKKKKKKK..",
                "..KBBYYYYYYBBK..",
                "..KBBBBBBBBBBK..",
                "..KKKKKKKKKKKK..",
            )
        ),
    )

    init {
        // All lists share the pacing in [artIndex].
        require(NORMAL_ARTS.size == ARTS.size && HARD_ARTS.size == ARTS.size)
    }

    fun arts(difficulty: Difficulty) = when (difficulty) {
        Difficulty.EASY -> ARTS
        Difficulty.NORMAL -> NORMAL_ARTS
        Difficulty.HARD -> HARD_ARTS
    }

    /** Level on which picture i of [ARTS] first appears: 1, 2, 4, 7, 10, ... */
    private fun introduction(picture: Int) = if (picture < 2) picture + 1 else 3 * picture - 2

    private val artIndex = HashMap<Int, Int>()

    fun artIndex(number: Int): Int = artIndex.getOrPut(number) {
        val new = ARTS.indices.firstOrNull { introduction(it) == number }
        if (new != null) return@getOrPut new
        // Reuse the known picture that was seen longest ago.
        val known = ARTS.indices.filter { introduction(it) < number }
        known.minBy { art -> (number - 1 downTo 1).firstOrNull { artIndex(it) == art } ?: 0 }
    }

    fun art(number: Int, difficulty: Difficulty): PixelArt = arts(difficulty)[artIndex(number)]

    /** True if this level is where its picture shows up for the first time. */
    fun isNewPicture(number: Int) = introduction(artIndex(number)) == number

    fun config(number: Int, difficulty: Difficulty): LevelConfig {
        val art = art(number, difficulty)
        val scale = when {
            number == 1 -> 1
            number <= 9 -> 2
            else -> 3
        }
        val maxVolume = when (scale) {
            1 -> 20
            2 -> 60
            else -> 100
        }
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
