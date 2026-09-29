package de.powerizzle.coloreater.ui

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import de.powerizzle.coloreater.game.Difficulty
import de.powerizzle.coloreater.game.Level
import de.powerizzle.coloreater.game.LevelCodec
import de.powerizzle.coloreater.game.LevelConfig
import de.powerizzle.coloreater.game.LevelGenerator
import de.powerizzle.coloreater.game.Levels
import de.powerizzle.coloreater.game.PixelArt
import de.powerizzle.coloreater.game.Status
import java.io.IOException
import java.util.concurrent.Executors
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Draws and runs the whole game: level menu, board and result overlay. */
class GameView(context: Context) : View(context) {
    private enum class Screen { TITLE, SETTINGS, MENU, LOADING, PLAYING }

    private val progress = context.getSharedPreferences("progress", Context.MODE_PRIVATE)
    private var difficulty: Difficulty =
        Difficulty.entries.firstOrNull { it.name == progress.getString(KEY_DIFFICULTY, null) } ?: Difficulty.NORMAL
        set(value) {
            field = value
            progress.edit { putString(KEY_DIFFICULTY, value.name) }
        }

    /** Highest level that may be played; everything below it is completed. Kept per difficulty. */
    private var unlocked: Int
        get() = unlocked(difficulty)
        set(value) = progress.edit { putInt(KEY_UNLOCKED + difficulty.name, value) }

    private fun unlocked(difficulty: Difficulty) = progress.getInt(
        KEY_UNLOCKED + difficulty.name,
        // Progress from before difficulties existed counts as Normal.
        if (difficulty == Difficulty.NORMAL) progress.getInt(KEY_UNLOCKED_OLD, 1) else 1,
    )

    // Levels are generated in the background; the next level is prepared while playing.
    private val generator = Executors.newSingleThreadExecutor()
    private val prepared = HashMap<Pair<Difficulty, Int>, Level>()
    private val preparing = HashSet<Pair<Difficulty, Int>>()

    private var screen = Screen.TITLE
    private var loadingNumber = 0
    private var session: Session? = null
    private var menuPage = (unlocked - 1) / LEVELS_PER_PAGE

    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
    }
    private val text = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textAlign = Paint.Align.CENTER
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    private val pixels = Paint().apply { isFilterBitmap = false }
    private val versionName: String =
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: ""
    private val thumbnails = HashMap<PixelArt, Bitmap>()

    private fun thumbnail(art: PixelArt) = thumbnails.getOrPut(art) {
        Bitmap.createBitmap(art.width, art.height, Bitmap.Config.ARGB_8888).apply {
            for (y in 0 until art.height) for (x in 0 until art.width) {
                val c = art.rows[y][x]
                if (c != '.') setPixel(x, y, PixelArt.PALETTE.getValue(c))
            }
        }
    }

    /** Layout unit: 1% of the view width. */
    private var u = 1f
    private val flowerRect = RectF()
    private val flower = FlowerIntro()
    private val menuButton = RectF()
    private val backButton = RectF()
    private val restartButton = RectF()
    private val settingsButton = RectF()
    private val resetButtons = Difficulty.entries.map { it to RectF() }
    private val pictureRect = RectF()
    private var cell = 1f
    private var slotRects: List<RectF> = emptyList()
    private var queueRects: List<RectF> = emptyList()
    private val primaryButton = RectF()
    private val secondaryButton = RectF()
    private val levelButtons = ArrayList<Pair<Int, RectF>>()
    private val difficultyButtons = Difficulty.entries.map { it to RectF() }
    private val previousPage = RectF()
    private val nextPage = RectF()
    private val scratch = RectF()
    private val badge = RectF()

    /** The title flower's ants live just off the screen edge, in the direction the flower picked. */
    private val flowerGeometry = object : AntSwarm.Geometry {
        private val cell get() = flowerRect.width() / FlowerIntro.SIZE
        override fun pixelX(pixel: Int) = flowerRect.left + (pixel % FlowerIntro.SIZE + 0.5f) * cell
        override fun pixelY(pixel: Int) = flowerRect.top + (pixel / FlowerIntro.SIZE + 0.5f) * cell
        override fun nestX(slot: Int) = flowerRect.centerX() + cos(flower.antDirection).toFloat() * nestDistance()
        override fun nestY(slot: Int) = flowerRect.centerY() + sin(flower.antDirection).toFloat() * nestDistance()
        override val antSpeed get() = 70 * u
        override val nestWidth get() = 10 * u

        /** From the flower's center to a bit beyond the screen edge. */
        private fun nestDistance(): Float {
            val dx = cos(flower.antDirection)
            val dy = sin(flower.antDirection)
            val toX = if (dx > 0) width - flowerRect.centerX() else flowerRect.centerX()
            val toY = if (dy > 0) height - flowerRect.centerY() else flowerRect.centerY()
            return min(toX / max(abs(dx), 1e-6), toY / max(abs(dy), 1e-6)).toFloat() + 5 * u
        }
    }

    private val geometry = object : AntSwarm.Geometry {
        private val picture get() = session!!.level.picture
        override fun pixelX(pixel: Int) = pictureRect.left + (picture.column(pixel) + 0.5f) * cell
        override fun pixelY(pixel: Int) = pictureRect.top + (picture.row(pixel) + 0.5f) * cell
        override fun nestX(slot: Int) = slotRects[slot].centerX()
        override fun nestY(slot: Int) = slotRects[slot].top + slotRects[slot].height() * 0.3f
        override val antSpeed get() = height * 0.45f
        override val nestWidth get() = slotRects.firstOrNull()?.width() ?: 0f
    }

    private fun now() = System.nanoTime() / 1e9

    // region Levels

    private fun startLevel(number: Int) {
        loadingNumber = number
        val level = prepared.remove(difficulty to number)
        if (level != null) {
            begin(level)
        } else {
            screen = Screen.LOADING
            prepare(number)
            invalidate()
        }
    }

    private fun prepare(number: Int) {
        val key = difficulty to number
        if (key in prepared || !preparing.add(key)) return
        generator.execute {
            val start = System.nanoTime()
            val config = Levels.config(number, key.first)
            val level = shipped(config) ?: LevelGenerator.generate(config)
            Log.d(TAG, "Prepared ${key.first} level $number in ${(System.nanoTime() - start) / 1_000_000} ms")
            post {
                preparing.remove(key)
                if (screen == Screen.LOADING && loadingNumber == number && difficulty == key.first) {
                    begin(level)
                } else {
                    prepared[key] = level
                }
            }
        }
    }

    /** Pre-generated level from assets; generating on the phone is slow, especially in debug builds. */
    private fun shipped(config: LevelConfig): Level? = try {
        val text = context.assets.open(LevelCodec.assetPath(config.difficulty, config.number))
            .bufferedReader().use { it.readText() }
        LevelCodec.decode(config, text)
    } catch (_: IOException) {
        null
    }

    /** Starts [level], continuing where it was left if it was saved and [resume] is set. */
    private fun begin(level: Level, resume: Boolean = true) {
        session = Session(level).apply {
            if (resume) savedPicks(level.config)?.let(::restore) else forget(level.config.difficulty)
        }
        screen = Screen.PLAYING
        layoutBoard()
        prepare(level.config.number + 1)
        invalidate()
    }

    // The level in progress is saved as the queues picked so far. The rules are deterministic, so
    // playing them again restores it exactly. A save from another app version is dropped, because
    // the level may have been generated differently.

    private fun save(s: Session) {
        val config = s.level.config
        if (s.state.status != Status.PLAYING) {
            forget(config.difficulty)
            return
        }
        val saved = "$versionName|${config.number}|${s.picks.joinToString(",")}"
        progress.edit { putString(KEY_SAVED + config.difficulty.name, saved) }
    }

    private fun savedPicks(config: LevelConfig): List<Int>? {
        val parts = progress.getString(KEY_SAVED + config.difficulty.name, null)?.split("|") ?: return null
        if (parts.size != 3 || parts[0] != versionName || parts[1] != config.number.toString()) return null
        return parts[2].split(",").filter { it.isNotEmpty() }.map { it.toIntOrNull() ?: return null }
    }

    private fun forget(difficulty: Difficulty) = progress.edit { remove(KEY_SAVED + difficulty.name) }

    /** Locks all levels of [choice] again, after asking. */
    private fun confirmReset(choice: Difficulty) {
        AlertDialog.Builder(context)
            .setTitle("Reset ${choice.label}?")
            .setMessage("All ${choice.label} levels lock again and you start over at level 1.")
            .setNegativeButton("Cancel", null)
            .setPositiveButton("Reset") { _, _ ->
                // Written rather than removed: Normal would otherwise fall back to the old key.
                progress.edit { putInt(KEY_UNLOCKED + choice.name, 1) }
                forget(choice)
                invalidate()
            }
            .show()
    }

    /** Returns false if the back press should close the app. */
    fun onBackPressed(): Boolean {
        when (screen) {
            Screen.TITLE -> return false
            Screen.SETTINGS, Screen.MENU -> showTitle()
            else -> showMenu()
        }
        return true
    }

    private fun showTitle() {
        screen = Screen.TITLE
        invalidate()
    }

    private fun showSettings() {
        screen = Screen.SETTINGS
        invalidate()
    }

    private fun showMenu() {
        screen = Screen.MENU
        session = null
        menuPage = (unlocked - 1) / LEVELS_PER_PAGE
        layoutMenu()
        invalidate()
    }

    // endregion
    // region Layout

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        u = w / 100f
        layoutTitle()
        layoutSettings()
        layoutMenu()
        layoutBoard()
    }

    private fun safeTop(): Float = max(
        (ViewCompat.getRootWindowInsets(this)?.displayCutout?.safeInsetTop ?: 0).toFloat(),
        3 * u,
    )

    /** Title, a big flower, and one button per difficulty below it. */
    private fun layoutTitle() {
        val buttonHeight = 15 * u
        val gap = 4 * u
        val buttonsTop = height - 14 * u - 3 * buttonHeight - 2 * gap
        for ((i, choice) in difficultyButtons.withIndex()) {
            val top = buttonsTop + i * (buttonHeight + gap)
            choice.second.set(16 * u, top, width - 16 * u, top + buttonHeight)
        }
        settingsButton.set(width - 13 * u, safeTop(), width - 3 * u, safeTop() + 10 * u)
        val areaTop = safeTop() + 30 * u
        val areaBottom = buttonsTop - 6 * u
        val flowerSize = min(width - 12 * u, areaBottom - areaTop)
        val flowerTop = (areaTop + areaBottom - flowerSize) / 2
        flowerRect.set((width - flowerSize) / 2, flowerTop, (width + flowerSize) / 2, flowerTop + flowerSize)
    }

    /** One row per difficulty, each with its reset button on the right. */
    private fun layoutSettings() {
        val top = safeTop() + 36 * u
        for ((i, pair) in resetButtons.withIndex()) {
            val y = top + i * 16 * u
            pair.second.set(width - 30 * u, y, width - 6 * u, y + 11 * u)
        }
    }

    private fun layoutMenu() {
        val header = safeTop()
        backButton.set(4 * u, header, 16 * u, header + 12 * u)
        levelButtons.clear()
        val columns = 4
        val gap = 3 * u
        val buttonWidth = (width - 8 * u - gap * (columns - 1)) / columns
        val buttonHeight = buttonWidth * 1.2f
        val top = header + 18 * u
        // Only a couple of locked levels are teased; the rest of the page stays empty.
        val lastShown = unlocked + LOCKED_SHOWN
        for (i in 0 until LEVELS_PER_PAGE) {
            val number = menuPage * LEVELS_PER_PAGE + i + 1
            if (number > lastShown) break
            val left = 4 * u + (i % columns) * (buttonWidth + gap)
            val y = top + (i / columns) * (buttonHeight + gap)
            levelButtons += number to RectF(left, y, left + buttonWidth, y + buttonHeight)
        }
        val arrowTop = top + 5 * (buttonHeight + gap) + 2 * u
        previousPage.set(4 * u, arrowTop, 30 * u, arrowTop + 12 * u)
        nextPage.set(70 * u, arrowTop, 96 * u, arrowTop + 12 * u)
    }

    private fun layoutBoard() {
        val s = session ?: return
        if (width == 0) return
        val top = safeTop()
        menuButton.set(4 * u, top, 16 * u, top + 12 * u)
        restartButton.set(width - 16 * u, top, width - 4 * u, top + 12 * u)

        val picture = s.level.picture
        val areaTop = top + 17 * u
        val areaBottom = height * 0.52f
        val areaWidth = width - 8 * u
        val areaHeight = areaBottom - areaTop
        cell = min(areaWidth / picture.width, areaHeight / picture.height)
        val pictureWidth = cell * picture.width
        val pictureHeight = cell * picture.height
        val left = (width - pictureWidth) / 2
        val pictureTop = areaTop + (areaHeight - pictureHeight) / 2
        pictureRect.set(left, pictureTop, left + pictureWidth, pictureTop + pictureHeight)

        val slotCount = s.state.slots.size
        val gap = 3 * u
        val slotSize = min((width - 8 * u - gap * (slotCount - 1)) / slotCount, height * 0.085f)
        val rowLeft = (width - slotSize * slotCount - gap * (slotCount - 1)) / 2
        val slotTop = areaBottom + 4 * u
        slotRects = List(slotCount) { i ->
            val x = rowLeft + i * (slotSize + gap)
            RectF(x, slotTop, x + slotSize, slotTop + slotSize)
        }

        val queueCount = s.state.queues.size
        val queueTop = slotTop + slotSize + 5 * u
        val queueWidth = (width - 8 * u - gap * (queueCount - 1)) / queueCount
        queueRects = List(queueCount) { i ->
            val x = 4 * u + i * (queueWidth + gap)
            RectF(x, queueTop, x + queueWidth, height - 4 * u)
        }
    }

    // endregion
    // region Drawing

    override fun onDraw(canvas: Canvas) {
        canvas.drawColor(BACKGROUND)
        when (screen) {
            Screen.TITLE -> drawTitle(canvas)
            Screen.SETTINGS -> drawSettings(canvas)
            Screen.MENU -> drawMenu(canvas)
            Screen.LOADING -> drawLoading(canvas)
            Screen.PLAYING -> drawBoard(canvas, session!!)
        }
    }

    private fun drawTitle(canvas: Canvas) {
        val now = now()
        flower.update(now, flowerGeometry)

        text.color = INK
        fitText("Color Eater", 14 * u, width - 8 * u)
        canvas.drawText("Color Eater", width / 2f, safeTop() + 17 * u, text)
        text.color = INK_SOFT
        fitText("Send the ants, clear the picture", 4.5f * u, width - 8 * u)
        canvas.drawText("Send the ants, clear the picture", width / 2f, safeTop() + 25 * u, text)

        for ((choice, rect) in difficultyButtons) {
            val main = choice == difficulty
            fill.color = if (main) INK else PANEL
            canvas.drawRoundRect(rect, 4 * u, 4 * u, fill)
            text.color = if (main) Color.WHITE else INK
            text.textSize = 6 * u
            canvas.drawText(choice.label, rect.centerX(), rect.centerY() + 0.5f * u, text)
            text.color = if (main) PANEL else INK_SOFT
            text.textSize = 3.4f * u
            canvas.drawText("Level ${unlocked(choice)}", rect.centerX(), rect.centerY() + 5 * u, text)
        }

        drawCogIcon(canvas, settingsButton)

        text.color = INK_SOFT
        text.textSize = 3.2f * u
        canvas.drawText("Created with AI", width / 2f, height - 5 * u, text)
        text.textSize = 2.4f * u
        canvas.drawText("v$versionName", width / 2f, height - 2 * u, text)

        // Last, so ants walk over everything.
        val cell = flowerRect.width() / FlowerIntro.SIZE
        flower.draw(canvas, flowerRect, now, (cell * 1.8f).coerceIn(3.5f * u, 7f * u), min(cell, 3f * u))
        postInvalidateOnAnimation()
    }

    private fun fitText(label: String, size: Float, maxWidth: Float) {
        text.textSize = size
        val width = text.measureText(label)
        if (width > maxWidth) text.textSize = size * maxWidth / width
    }

    private fun drawSettings(canvas: Canvas) {
        drawBackIcon(canvas, backButton)
        text.color = INK
        text.textSize = 6.5f * u
        canvas.drawText("Settings", width / 2f, centerTextY(backButton.centerY()), text)

        text.textAlign = Paint.Align.LEFT
        text.color = INK_SOFT
        text.textSize = 4 * u
        canvas.drawText("PROGRESS", 6 * u, resetButtons.first().second.top - 5 * u, text)
        for ((choice, rect) in resetButtons) {
            text.textAlign = Paint.Align.LEFT
            text.color = INK
            text.textSize = 5.5f * u
            canvas.drawText(choice.label, 6 * u, rect.centerY() - 0.5f * u, text)
            text.color = INK_SOFT
            text.textSize = 3.4f * u
            canvas.drawText("Level ${unlocked(choice)}", 6 * u, rect.centerY() + 4 * u, text)
            text.textAlign = Paint.Align.CENTER
            text.textSize = 4.5f * u
            // Nothing to reset before the first level is won.
            val active = unlocked(choice) > 1
            drawButton(canvas, rect, "Reset", if (active) PANEL else LOCKED, if (active) INK else INK_SOFT)
        }
    }

    private fun drawMenu(canvas: Canvas) {
        drawBackIcon(canvas, backButton)
        text.color = INK
        text.textSize = 6.5f * u
        canvas.drawText(difficulty.label, width / 2f, centerTextY(backButton.centerY()), text)

        val reached = unlocked
        for ((number, rect) in levelButtons) {
            when {
                // Completed: the picture is the reward, so only these show it.
                number < reached -> {
                    fill.color = PANEL
                    canvas.drawRoundRect(rect, 3 * u, 3 * u, fill)
                    val art = Levels.pick(number, difficulty).art
                    val thumb = thumbnail(art)
                    // Pictures are not all square: fit them into the square above the number.
                    val box = rect.width() * 0.62f
                    val size = box / maxOf(art.width, art.height)
                    val w = size * art.width
                    val h = size * art.height
                    val top = rect.top + 3 * u + (box - h) / 2
                    scratch.set(rect.centerX() - w / 2, top, rect.centerX() + w / 2, top + h)
                    canvas.drawBitmap(thumb, null, scratch, pixels)
                }
                number == reached -> {
                    fill.color = PANEL
                    canvas.drawRoundRect(rect, 3 * u, 3 * u, fill)
                    stroke.color = ACCENT
                    stroke.strokeWidth = 0.8f * u
                    canvas.drawRoundRect(rect, 3 * u, 3 * u, stroke)
                    text.color = ACCENT
                    text.textSize = rect.width() * 0.4f
                    canvas.drawText("?", rect.centerX(), rect.top + rect.width() * 0.5f, text)
                    if (Levels.pick(number, difficulty).isNew) {
                        text.textSize = 3.2f * u
                        canvas.drawText("NEW", rect.centerX(), rect.top + 4.5f * u, text)
                    }
                }
                else -> {
                    fill.color = LOCKED
                    canvas.drawRoundRect(rect, 3 * u, 3 * u, fill)
                    drawLock(canvas, rect.centerX(), rect.top + rect.width() * 0.4f, rect.width() * 0.22f)
                }
            }
            text.color = if (number <= reached) INK else INK_SOFT
            text.textSize = 5 * u
            canvas.drawText(number.toString(), rect.centerX(), rect.bottom - 3 * u, text)
        }

        text.textSize = 7 * u
        if (menuPage > 0) drawButton(canvas, previousPage, "‹", PANEL, INK)
        if (menuPage < (reached - 1) / LEVELS_PER_PAGE) drawButton(canvas, nextPage, "›", PANEL, INK)

    }

    private fun drawLock(canvas: Canvas, cx: Float, cy: Float, size: Float) {
        stroke.color = INK_SOFT
        stroke.strokeWidth = size * 0.18f
        scratch.set(cx - size * 0.32f, cy - size * 0.75f, cx + size * 0.32f, cy)
        canvas.drawArc(scratch, 180f, 180f, false, stroke)
        canvas.drawLine(scratch.left, scratch.centerY(), scratch.left, cy, stroke)
        canvas.drawLine(scratch.right, scratch.centerY(), scratch.right, cy, stroke)
        fill.color = INK_SOFT
        canvas.drawRoundRect(cx - size * 0.5f, cy - size * 0.1f, cx + size * 0.5f, cy + size * 0.6f, size * 0.12f, size * 0.12f, fill)
    }

    private fun drawLoading(canvas: Canvas) {
        text.color = INK
        text.textSize = 6 * u
        canvas.drawText("Preparing level $loadingNumber…", width / 2f, height / 2f, text)
    }

    private fun drawBoard(canvas: Canvas, s: Session) {
        val now = now()
        s.update(now, geometry)
        val config = s.level.config

        // Top bar
        drawMenuIcon(canvas, menuButton)
        text.textSize = 7 * u
        drawButton(canvas, restartButton, "↻", PANEL, INK)
        text.color = INK
        text.textSize = 5.5f * u
        canvas.drawText("Level ${config.number} · ${config.art.name}", width / 2f, centerTextY(menuButton.centerY()), text)
        text.color = INK_SOFT
        text.textSize = 3.3f * u
        val hint = buildList {
            add(Levels.pick(config.number, config.difficulty).category.name)
            add(config.difficulty.label)
            add("${s.state.slots.size} slots")
            if (config.slack == 0 && config.number > 1) add("no room for mistakes")
            if (config.mystery > 0) add("? shows at the front")
        }.joinToString(" · ")
        canvas.drawText(hint, width / 2f, menuButton.bottom + 1.5f * u, text)

        // Picture
        fill.color = PANEL
        scratch.set(pictureRect)
        scratch.inset(-2 * u, -2 * u)
        canvas.drawRoundRect(scratch, 3 * u, 3 * u, fill)
        canvas.drawBitmap(s.bitmap, null, pictureRect, pixels)

        // Immediate queue
        val stuck = s.state.status == Status.LOST
        for ((slot, rect) in slotRects.withIndex()) {
            val view = s.slotView(slot)
            if (view == null) {
                fill.color = SLOT
                canvas.drawRoundRect(rect, rect.width() * 0.2f, rect.width() * 0.2f, fill)
                stroke.color = SLOT_EDGE
                stroke.strokeWidth = 0.6f * u
                canvas.drawRoundRect(rect, rect.width() * 0.2f, rect.width() * 0.2f, stroke)
            } else {
                drawVolume(canvas, rect, s.level.palette[view.volume.color], view.count, if (view.finishing) 110 else 255)
                // The volume that takes over once these ants are home waits in the corner.
                val next = view.incoming
                if (next != null) {
                    val size = rect.width() * 0.5f
                    badge.set(rect.right - size * 0.75f, rect.top - size * 0.25f, rect.right + size * 0.25f, rect.top + size * 0.75f)
                    drawVolume(canvas, badge, s.level.palette[next.color], next.count, 255)
                }
            }
            if (stuck && s.settledAt >= 0) {
                stroke.color = ACCENT
                stroke.strokeWidth = 1f * u
                canvas.drawRoundRect(rect, rect.width() * 0.2f, rect.width() * 0.2f, stroke)
            }
        }

        // Volume queues
        for ((queue, rect) in queueRects.withIndex()) {
            val since = now - s.rejectedAt[queue]
            val shake = if (since < 0.35) (sin(since * 45) * exp(-since * 8) * 2 * u).toFloat() else 0f
            canvas.save()
            canvas.translate(shake, 0f)
            drawQueue(canvas, s, queue, rect)
            canvas.restore()
        }

        val antLength = (cell * 1.8f).coerceIn(3.5f * u, 6f * u)
        s.swarm.draw(canvas, now, antLength, cell.coerceIn(1.2f * u, 2.6f * u), s.level.palette)

        val settled = s.settledAt >= 0
        if (settled && s.state.status == Status.WON && unlocked <= config.number) unlocked = config.number + 1
        val overlay = settled && now - s.settledAt > OVERLAY_DELAY
        if (overlay) drawResult(canvas, s)

        val shaking = s.rejectedAt.any { now - it < 0.35 }
        if (!s.swarm.isIdle || shaking || (s.state.status != Status.PLAYING && !overlay)) postInvalidateOnAnimation()
    }

    private fun drawQueue(canvas: Canvas, s: Session, queue: Int, rect: RectF) {
        fill.color = PANEL
        canvas.drawRoundRect(rect, 3 * u, 3 * u, fill)
        val volumes = s.state.queues[queue]
        // Volumes keep their true color and get smaller the further back they are, so a long
        // queue still fits; the smallest ones drop their count.
        var size = min(rect.width() * 0.78f, slotRects.first().width())
        var y = rect.top + 2.5f * u
        var shown = 0
        for ((i, volume) in volumes.withIndex()) {
            val bottom = if (i < volumes.size - 1) rect.bottom - 7 * u else rect.bottom - 2 * u
            if (size < 2.5f * u || y + size > bottom) break
            scratch.set(rect.centerX() - size / 2, y, rect.centerX() + size / 2, y + size)
            if (volume.hidden && i > 0) {
                drawMystery(canvas, scratch)
            } else {
                drawVolume(canvas, scratch, s.level.palette[volume.color], volume.count, 255, label = size >= 7 * u)
            }
            y += size * 1.15f
            size *= 0.82f
            shown++
        }
        if (shown < volumes.size) {
            text.color = INK_SOFT
            text.textSize = 4 * u
            canvas.drawText("+${volumes.size - shown}", rect.centerX(), rect.bottom - 2 * u, text)
        }
    }

    private fun drawVolume(canvas: Canvas, rect: RectF, color: Int, count: Int, alpha: Int, label: Boolean = true) {
        val radius = rect.width() * 0.2f
        fill.color = color
        fill.alpha = alpha
        canvas.drawRoundRect(rect, radius, radius, fill)
        stroke.color = ColorUtils.blendARGB(color, Color.BLACK, 0.3f)
        stroke.alpha = alpha
        stroke.strokeWidth = rect.width() * 0.05f
        scratch.set(rect)
        scratch.inset(stroke.strokeWidth / 2, stroke.strokeWidth / 2)
        canvas.drawRoundRect(scratch, radius, radius, stroke)
        if (!label) return
        text.color = if (Color.luminance(color) > 0.5f) INK else Color.WHITE
        text.alpha = alpha
        text.textSize = rect.height() * 0.42f
        canvas.drawText(count.toString(), rect.centerX(), centerTextY(rect.centerY()), text)
        text.alpha = 255
    }

    private fun drawMystery(canvas: Canvas, rect: RectF) {
        val radius = rect.width() * 0.2f
        fill.color = MYSTERY
        canvas.drawRoundRect(rect, radius, radius, fill)
        text.color = Color.WHITE
        text.textSize = rect.height() * 0.5f
        canvas.drawText("?", rect.centerX(), centerTextY(rect.centerY()), text)
    }

    private fun drawResult(canvas: Canvas, s: Session) {
        canvas.drawColor(0x88000000.toInt())
        val won = s.state.status == Status.WON
        val panel = RectF(8 * u, height * 0.32f, width - 8 * u, height * 0.32f + 62 * u)
        fill.color = BACKGROUND
        canvas.drawRoundRect(panel, 4 * u, 4 * u, fill)
        text.color = if (won) INK else ACCENT
        text.textSize = 9 * u
        canvas.drawText(if (won) "Picture cleared!" else "Stuck!", width / 2f, panel.top + 15 * u, text)
        text.color = INK_SOFT
        text.textSize = 4.2f * u
        val detail = if (won) "The ants carried away every pixel." else "All slots are full and nothing can be eaten."
        canvas.drawText(detail, width / 2f, panel.top + 24 * u, text)

        primaryButton.set(panel.left + 6 * u, panel.top + 32 * u, panel.right - 6 * u, panel.top + 44 * u)
        secondaryButton.set(panel.left + 6 * u, panel.top + 47 * u, panel.right - 6 * u, panel.top + 57 * u)
        text.textSize = 5.5f * u
        drawButton(canvas, primaryButton, if (won) "Next level" else "Try again", ACCENT, Color.WHITE)
        drawButton(canvas, secondaryButton, "Levels", PANEL, INK)
    }

    private fun drawButton(canvas: Canvas, rect: RectF, label: String, background: Int, foreground: Int) {
        fill.color = background
        canvas.drawRoundRect(rect, 3 * u, 3 * u, fill)
        text.color = foreground
        canvas.drawText(label, rect.centerX(), centerTextY(rect.centerY()), text)
    }

    private fun drawBackIcon(canvas: Canvas, rect: RectF) {
        fill.color = PANEL
        canvas.drawRoundRect(rect, 3 * u, 3 * u, fill)
        stroke.color = INK
        stroke.strokeWidth = 1f * u
        val cx = rect.centerX() + 0.8f * u
        val cy = rect.centerY()
        canvas.drawLine(cx, cy - 3 * u, cx - 3 * u, cy, stroke)
        canvas.drawLine(cx - 3 * u, cy, cx, cy + 3 * u, stroke)
    }

    private fun drawCogIcon(canvas: Canvas, rect: RectF) {
        fill.color = PANEL
        canvas.drawRoundRect(rect, 3 * u, 3 * u, fill)
        val cx = rect.centerX()
        val cy = rect.centerY()
        val r = rect.width() * 0.3f
        stroke.color = INK
        stroke.strokeWidth = r * 0.42f
        stroke.strokeCap = Paint.Cap.BUTT
        for (i in 0 until 8) {
            val a = i * PI / 4
            canvas.drawLine(
                cx + (cos(a) * r * 0.6f).toFloat(), cy + (sin(a) * r * 0.6f).toFloat(),
                cx + (cos(a) * r).toFloat(), cy + (sin(a) * r).toFloat(), stroke,
            )
        }
        stroke.strokeCap = Paint.Cap.ROUND
        fill.color = INK
        canvas.drawCircle(cx, cy, r * 0.72f, fill)
        fill.color = PANEL
        canvas.drawCircle(cx, cy, r * 0.3f, fill)
    }

    private fun drawMenuIcon(canvas: Canvas, rect: RectF) {
        fill.color = PANEL
        canvas.drawRoundRect(rect, 3 * u, 3 * u, fill)
        stroke.color = INK
        stroke.strokeWidth = 1f * u
        for (i in -1..1) {
            val y = rect.centerY() + i * 2.2f * u
            canvas.drawLine(rect.left + 3.5f * u, y, rect.right - 3.5f * u, y, stroke)
        }
    }

    private fun centerTextY(centerY: Float) = centerY - (text.descent() + text.ascent()) / 2

    // endregion
    // region Input

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.actionMasked == MotionEvent.ACTION_DOWN) tap(event.x, event.y)
        return true
    }

    private fun tap(x: Float, y: Float) {
        when (screen) {
            Screen.TITLE -> tapTitle(x, y)
            Screen.SETTINGS -> tapSettings(x, y)
            Screen.MENU -> tapMenu(x, y)
            Screen.LOADING -> Unit
            Screen.PLAYING -> tapBoard(session!!, x, y)
        }
    }

    private fun tapTitle(x: Float, y: Float) {
        if (settingsButton.contains(x, y)) {
            showSettings()
            return
        }
        val (choice, _) = difficultyButtons.firstOrNull { it.second.contains(x, y) } ?: return
        difficulty = choice
        showMenu()
    }

    private fun tapSettings(x: Float, y: Float) {
        if (backButton.contains(x, y)) {
            showTitle()
            return
        }
        val (choice, _) = resetButtons.firstOrNull { it.second.contains(x, y) } ?: return
        if (unlocked(choice) > 1) confirmReset(choice)
    }

    private fun tapMenu(x: Float, y: Float) {
        if (backButton.contains(x, y)) {
            showTitle()
            return
        }
        levelButtons.firstOrNull { it.second.contains(x, y) }?.let { (number, _) ->
            if (number <= unlocked) startLevel(number)
            return
        }
        val pages = (unlocked - 1) / LEVELS_PER_PAGE
        if (previousPage.contains(x, y) && menuPage > 0) menuPage--
        else if (nextPage.contains(x, y) && menuPage < pages) menuPage++
        else return
        layoutMenu()
        invalidate()
    }

    private fun tapBoard(s: Session, x: Float, y: Float) {
        val now = now()
        if (s.settledAt >= 0 && now - s.settledAt > OVERLAY_DELAY) {
            val number = s.level.config.number
            when {
                primaryButton.contains(x, y) ->
                    if (s.state.status == Status.WON) startLevel(number + 1) else begin(s.level, resume = false)
                secondaryButton.contains(x, y) -> showMenu()
            }
            return
        }
        when {
            menuButton.contains(x, y) -> showMenu()
            restartButton.contains(x, y) -> begin(s.level, resume = false)
            else -> {
                val queue = queueRects.indexOfFirst { it.contains(x, y) }
                if (queue >= 0 && s.pick(queue, now)) save(s)
            }
        }
        invalidate()
    }

    // endregion

    private companion object {
        const val TAG = "ColorEater"
        const val KEY_UNLOCKED = "unlocked_"
        const val KEY_UNLOCKED_OLD = "unlocked"
        const val KEY_DIFFICULTY = "difficulty"
        const val KEY_SAVED = "saved_"
        const val LOCKED_SHOWN = 2
        const val LEVELS_PER_PAGE = 20
        const val OVERLAY_DELAY = 0.5

        const val BACKGROUND = 0xFFF4EDE1.toInt()
        const val PANEL = 0xFFE6D9C3.toInt()
        const val LOCKED = 0xFFE9E3D9.toInt()
        const val SLOT = 0xFFEDE3D2.toInt()
        const val SLOT_EDGE = 0xFFC9B79C.toInt()
        const val INK = 0xFF3A2E2A.toInt()
        const val INK_SOFT = 0xFF8A7B70.toInt()
        const val ACCENT = 0xFFE0453A.toInt()
        const val MYSTERY = 0xFFA89A8C.toInt()
    }
}
