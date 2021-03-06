package com.lateinit.apps.equalide.activities

import android.net.Uri
import android.util.Log
import android.os.Bundle

import android.content.Intent
import android.content.Context
import android.content.pm.PackageManager

import androidx.core.view.GravityCompat
import androidx.core.content.ContextCompat
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.ActionBarDrawerToggle
import com.google.android.material.navigation.NavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton

import android.graphics.Color
import android.graphics.drawable.GradientDrawable

import android.text.SpannableString
import android.text.style.ForegroundColorSpan

import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewTreeObserver.OnGlobalLayoutListener

import android.widget.*

import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_screen.*

import com.lateinit.apps.equalide.R
import com.lateinit.apps.equalide.logic.Pack
import com.lateinit.apps.equalide.logic.Puzzle
import com.lateinit.apps.equalide.data.packsData

class Main : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    // Diffetent element sizes
    private val contentView = object {
        var width: Int = 0
        var height: Int = 0
    }
    private var colorPaletteSize: Int = 0
    private var primitiveSize: Int = 0

    // Paint related
    private var paintColor: Int = 0
    private var colors: IntArray? = null
    private var eraseMode: Boolean = false
    private var previousPaintCoords: IntArray = IntArray(2)

    // Puzzle related
    private var puzzle: Puzzle? = null
    private var packs: Array<Pack> = packsData.packs
    private val packSize: Int = 24
    private val packIds = arrayOf(
        R.id.pack_01, R.id.pack_02, R.id.pack_03,
        R.id.pack_04, R.id.pack_05, R.id.pack_06,
        R.id.pack_07, R.id.pack_08, R.id.pack_09
    )
    private var selectedPackInNav: Int = 0

    // Walkthrough related
    private val current = object {
        var level: Int = 0
        var pack: Int = 0
        var levelSolved: Boolean = false
    }
    private val levelOpeningDelta: Int = 3

    // Views related
    private var menu: Menu? = null
    private var fabIsShowed: Boolean = false
    private var fabIsLocked: Boolean = false
    private var fabIconChanged: Boolean = false
    private var grid: GridLayout? = null
    private var palette: LinearLayout? = null
    private var navigatedToSelectScreen: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // Add navigation drawer
        val toggle = object : ActionBarDrawerToggle(
            this, activity_main, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ) {
            // Uncheck selected item in navigation drawer after closing it
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                menu?.getItem(selectedPackInNav)?.isChecked = false
            }
        }
        activity_main.addDrawerListener(toggle)
        toggle.syncState()

        // Find or create views
        grid = findViewById(R.id.puzzle_grid)
        palette = findViewById(R.id.color_palette)
        menu = findViewById<NavigationView>(R.id.nav_view).menu

        // Add listeners to views
        grid?.setOnTouchListener(gridListener)
        nav_view.setNavigationItemSelectedListener(this)
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener(fabListener)

        loadUserData()

        // Listener to detect when layout is loaded to get it's resolution properties
        grid?.viewTreeObserver?.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                grid?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                onLayoutLoad()
            }
        })
    }

    // Close drawer on return from select level activity
    override fun onResume() {
        super.onResume()
        if (navigatedToSelectScreen && activity_main.isDrawerOpen(GravityCompat.START)) {
            navigatedToSelectScreen = false
            activity_main.closeDrawer(GravityCompat.START, false)
        }
    }

    // Launch new activity if selected proper item from navigation drawer
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        for (i in 0 until packIds.size)
            if (item.itemId == packIds[i]) {
                // Fixes issue when selected item becomes unselectable
                // on first select after closing of navigation drawer
                menu?.getItem(i)?.isChecked = true

                // Set color for text
                val spanString = SpannableString(item.title.toString())
                spanString.setSpan(
                    ForegroundColorSpan(Color.WHITE), 0, spanString.length, 0)
                item.title = spanString

                selectedPackInNav = i
                break
            }
        if (packs[selectedPackInNav].opened)
            launchSelectLevelActivity()
        return true
    }

    private fun launchMailApplication() {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:${resources
            .getString(R.string.contact_mail_address)}")
        intent.putExtra(Intent.EXTRA_SUBJECT, resources
            .getString(R.string.mail_title))
        intent.putExtra(Intent.EXTRA_TEXT, resources
            .getString(R.string.mail_body).replace("#",
                "${current.pack + 1}-${(current.level + 1).toString().padStart(2, '0')}"))

        // Get list of all installed mail clients on device that are not disabled
        val appList = this.packageManager.queryIntentActivities(intent,
            PackageManager.MATCH_DEFAULT_ONLY or PackageManager.GET_RESOLVED_FILTER)

        if (appList.size == 1) {
            intent.setClassName(
                appList[0].activityInfo.packageName,
                appList[0].activityInfo.name
            )
            // If there is only one mail client - launch it
            startActivity(intent)
        } else
            // If there is more than one mail client - offer choice
            startActivity(Intent.createChooser(intent, resources
                .getString(R.string.mail_chooser_title)))
    }

    private fun launchSelectLevelActivity() {
        // String that contains user progress in selected pack
        // 's' - solved level
        // 'o' - opened level
        // 'c' - closed level
        var levelData = ""

        for (level in packs[selectedPackInNav].puzzles)
            levelData += if (level.solved) "s" else if (level.opened) "o" else "c"

        val intent = Intent(this, SelectLevel::class.java).apply {
            putExtra("pack", (selectedPackInNav + 1).toString())
            putExtra("level data", levelData)
        }

        navigatedToSelectScreen = true
        startActivity(intent)

        overridePendingTransition(R.anim.left_right_enter, R.anim.left_right_exit)
    }

    // Parse data received from select level activity
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val selectedLevel = intent?.getIntExtra("selected level", -1)

        if (selectedLevel != null && selectedLevel != -1) {
            grid?.removeAllViews()
            palette?.removeAllViews()

            if (fabIsShowed) {
                findViewById<FloatingActionButton>(R.id.fab).hide()
                saveFabStatus(false)
            }

            current.level = selectedLevel
            current.pack = selectedPackInNav
            current.levelSolved = false
            saveCurrentSelectedLevel()

            paintColor = packs[current.pack].puzzles[current.level].parts / 2
            savePaletteStatus()

            packs[current.pack].puzzles[current.level].refresh()

            onLayoutLoad()
        }
    }

    // Close navigation drawer on back button pressed
    override fun onBackPressed() {
        if (activity_main.isDrawerOpen(GravityCompat.START)) {
            activity_main.closeDrawer(GravityCompat.START)
        } else
            super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.actionbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.send_mail -> launchMailApplication()
            R.id.refresh_button -> refreshGrid()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    fun onLayoutLoad() {
        supportActionBar?.title = "Equalide   ${current.pack + 1}-${(current.level + 1)
            .toString().padStart(2, '0')}"
        calculateViewsSizes()

        puzzle = packs[current.pack].puzzles[current.level]
        colors = resources.getIntArray(resources.getIdentifier(
                "colors_for_${puzzle!!.parts}_parts",
                "array", this.packageName))

        addColorPalette(colors!!)

        // Happens when app reloads on solved level state
        if (fabIsShowed)
            hideColorPalette()

        renderPuzzle(puzzle!!)
    }

//    private fun loadPacks(): Array<Pack> {
//        val array = Array(packIds.size, { _ -> ArrayList<Puzzle>(0) })
//        var file: InputStream? = null
//        var parts = 2
//
//        for (i in 1..packIds.size)
//            for (j in 1..packSize) {
//                try {
//                    file = assets.open("$i/${j.toString().padStart(2, '0')}-$parts.txt")
//                } catch (_: Exception) {
//                    try {
//                        parts++
//                        file = assets.open("$i/${j.toString().padStart(2, '0')}-$parts.txt")
//                    } catch (e: Exception) {
//                        Log.d("ERROR", e.toString())
//                    }
//                }
//                array[i - 1].add(Puzzle(file!!.bufferedReader().use { it.readText() }, parts))
//            }
//
//        return Array(packIds.size, { i -> Pack(array[i].toTypedArray()) })
//    }

    private fun loadUserData() {
        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        // Get user progress
//        val packProgress = "ssssssss"
//        val levelProgress = ("s".repeat(24) + "\n").repeat(7) + "s".repeat(23) + "o\n"
        val packProgress = preferences.getString("Pack progress", null)
        val levelProgress = preferences.getString("Level progress", null)
        val levelPartition = preferences.getString("Level partition", null)
        val currentPosition = preferences.getString("Current position", null)

        // Get state variables
        paintColor = preferences.getInt("Palette status", 0)
        fabIsShowed = preferences.getBoolean("Fab status", false)

        if (packProgress == null || levelProgress == null) {
            // Open first pack and three levels if it's new game
            packs[0].opened = true

            for (i in 0 until levelOpeningDelta)
                packs[0].puzzles[i].opened = true
        } else {
            // Load pack progress and change icons in navigation drawer
            for (i in 0 until packProgress.length) {
                when (packProgress[i]) {
                    's' -> {
                        packs[i].opened = true
                        packs[i].solved = true
                        menu?.findItem(packIds[i])?.icon =
                                ContextCompat.getDrawable(this, R.drawable.ic_star)
                    }
                    'o' -> {
                        packs[i].opened = true
                        menu?.findItem(packIds[i])?.icon =
                                ContextCompat.getDrawable(this, R.drawable.ic_lock_open)
                    }
                }
            }

            // Load level progress
            val levelData = levelProgress.split("\n")

            for (i in 0 until levelData.size - 1)
                for (j in 0 until levelData[i].length) {
                    when (levelData[i][j]) {
                        's' -> {
                            packs[i].puzzles[j].opened = true
                            packs[i].puzzles[j].solved = true
                        }
                        'o' -> packs[i].puzzles[j].opened = true
                    }
                }

            // Open 9 pack
            if (packProgress.length == 8 && packProgress[7] != 'c' && packs[7].puzzles[21].solved) {
                packs[8].opened = true
                menu?.findItem(packIds[8])?.icon =
                        ContextCompat.getDrawable(this, R.drawable.ic_lock_open)
                var forOpen = 0
                for (i in 21 until 24)
                    if (packs[7].puzzles[i].solved)
                        forOpen++

                for (i in 0 until  forOpen)
                    packs[8].puzzles[i].opened = true

                saveProgress()
            }
        }

        // Load current level
        if (currentPosition != null) {
            current.pack = currentPosition.substring(0..0).toInt()
            current.level = currentPosition.substring(1).toInt()
        }

        // Load current level partition
        if (levelPartition != null) {
            val puzzle = packs[current.pack].puzzles[current.level]

            if (puzzle.width * puzzle.height == levelPartition.length &&
                    !levelPartition.any { c: Char -> !c.isDigit() && c != 'e' && c != 'b' })
                // Successful case
                puzzle.setPartition(levelPartition)
            else {
                // Data is corrupted, loads clean partition instead
                puzzle.refresh()
                Log.d("ERROR", "Incorrect load ocurred!")
            }
        }

        // Show fab if exited on opened fab
        if (fabIsShowed) {
            current.levelSolved = true
            findViewById<FloatingActionButton>(R.id.fab).show(onShownFabListener)
        }
    }

    private fun saveProgress() {
        var packProgress = ""
        var levelProgress = ""

        // Prepare pack progress
        for (pack in packs)
            packProgress += if (pack.solved) "s" else if (pack.opened) "o" else "c"

        // Prepare level progress
        for (pack in packs) {
            for (level in pack.puzzles)
                levelProgress += if (level.solved) "s" else if (level.opened) "o" else "c"
            levelProgress += "\n"
        }

        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        // Save all progress
        with(preferences.edit()) {
            putString("Pack progress", packProgress)
            putString("Level progress", levelProgress)
            apply()
        }
    }

    private fun savePartition() {
        val levelPartition = packs[current.pack].puzzles[current.level].getPartition()

        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        with(preferences.edit()) {
            putString("Level partition", levelPartition)
            apply()
        }
    }

    private fun saveCurrentSelectedLevel() {
        val currentPosition = current.pack.toString() + current.level.toString()

        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        with(preferences.edit()) {
            putString("Current position", currentPosition)
            apply()
        }
    }

    private fun saveFabStatus(status: Boolean) {
        fabIsShowed = status

        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        with(preferences.edit()) {
            putBoolean("Fab status", status)
            apply()
        }
    }

    private fun savePaletteStatus() {
        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        with(preferences.edit()) {
            putInt("Palette status", paintColor)
            apply()
        }
    }

    private fun calculateViewsSizes() {
        val contentView = findViewById<LinearLayout>(R.id.content_view)

        colorPaletteSize = contentView.width / 5

        this.contentView.width = contentView.width
        this.contentView.height = (contentView.height
                - resources.getDimension(R.dimen.puzzle_grid_margin_top)
                - resources.getDimension(R.dimen.puzzle_grid_margin_bottom)
                - colorPaletteSize).toInt()
    }

    private fun addColorPalette(paletteColors: IntArray) {
        for (i in paletteColors.indices) {
            val paletteButton = ImageButton(this)

            // Set size
            val params = LinearLayout.LayoutParams(
                this.colorPaletteSize,this.colorPaletteSize)
            paletteButton.layoutParams = params

            // Set color
            val drawable = ContextCompat.getDrawable(this,
                R.drawable.primitive_border) as GradientDrawable
            drawable.setColor(paletteColors[i])
            paletteButton.background = drawable

            // Set edit icon on background
            if (i == paintColor)
                paletteButton.setImageDrawable(
                    ContextCompat.getDrawable(this, R.drawable.ic_edit))

            paletteButton.tag = "paletteButton_$i"

            paletteButton.setOnClickListener(colorPaletteListener)

            palette?.addView(paletteButton)
        }
    }

    private fun renderPuzzle(puzzle: Puzzle) {
        primitiveSize = minOf(contentView.width / puzzle.width, contentView.height / puzzle.height)

        grid?.columnCount = puzzle.width
        grid?.rowCount = puzzle.height

        // Center-align puzzle between action bar and palette
        val gridParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        gridParams.bottomMargin = (contentView.height - puzzle.height * primitiveSize) / 2
        grid?.layoutParams = gridParams

        val blackPrimitiveColor = ContextCompat.getColor(this, R.color.acitivityBackground)

        for (i in 0 until puzzle.height)
            for (j in 0 until puzzle.width) {
                val primitive = Button(this)

                // Set size
                val params = LinearLayout.LayoutParams(primitiveSize, primitiveSize)
                primitive.layoutParams = params

                // Set color
                val drawable = if (puzzle[i, j] == 'b') GradientDrawable()
                    else ContextCompat.getDrawable(this, R.drawable.primitive_border)
                        as GradientDrawable
                drawable.setColor(
                    when (puzzle[i, j]) {
                        'b' -> blackPrimitiveColor
                        'e' -> Color.WHITE
                        else -> colors!![puzzle[i, j].toInt() - 48]
                    }
                )
                primitive.background = drawable

                primitive.tag = intArrayOf(i, j)

                // Clicks are toggled off to handle only touches, otherwise there will be duplicates
                primitive.isClickable = false
                primitive.setOnTouchListener { _: View, _: MotionEvent -> false }

                grid?.addView(primitive)
            }
    }

    private fun handleSolvedPuzzle() {
        hideColorPalette()

        // Is true for last level in game although no fab is actually shown.
        // It's because this setting is also used to save current level solved property.
        saveFabStatus(true)

        val allLevelsSolved = checkIfAllLevelsSolved()
        current.levelSolved = true

        if (!allLevelsSolved) {
            packs[current.pack].puzzles[current.level].solved = true

            if (!packs[current.pack].solved && packs[current.pack].checkIfSolved()) {
                packs[current.pack].solved = true
                menu?.findItem(packIds[current.pack])?.icon =
                        ContextCompat.getDrawable(this, R.drawable.ic_star)
            }

            openNextLevels()
            saveProgress()
        }

        val fab = findViewById<FloatingActionButton>(R.id.fab)

        if (fabIconChanged &&
            (current.level != packSize - 1 || current.pack != packIds.size - 1)) {
            fab.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_navigate_next))
            fabIconChanged = false
        }

        fab.show(onShownFabListener)
    }

    private fun refreshGrid() {
        puzzle?.refresh()
        savePartition()

        if (!current.levelSolved) {
            val blackPrimitiveColor = ContextCompat.getColor(this, R.color.acitivityBackground)

            for (i in 0 until grid!!.childCount) {
                val primitive = grid?.getChildAt(i) as Button
                val coords = primitive.tag as IntArray
                val background = primitive.background as GradientDrawable

                background.setColor(if (puzzle!![coords[0], coords[1]] == 'b') blackPrimitiveColor else Color.WHITE)
                primitive.background = background
            }
        } else {
            grid?.removeAllViews()
            palette?.removeAllViews()

            findViewById<FloatingActionButton>(R.id.fab).hide()
            saveFabStatus(false)

            current.levelSolved = false

            paintColor = packs[current.pack].puzzles[current.level].parts / 2
            savePaletteStatus()

            onLayoutLoad()
        }
    }

    private fun hideColorPalette() {
        for (i in 0 until palette!!.childCount)
            palette?.getChildAt(i)?.setBackgroundColor(resources.getColor(android.R.color.transparent))

        palette?.findViewWithTag<ImageButton>("paletteButton_$paintColor")
            ?.setImageResource(android.R.color.transparent)
    }

    private fun checkIfAllLevelsSolved(): Boolean {
        for (pack in packs)
            if (!pack.solved)
                return false

        return true
    }

    private fun openNextLevels() {
        // Open levels in current pack
        if (current.level <= packSize - 1 - levelOpeningDelta) {
            for (i in 1..levelOpeningDelta)
                packs[current.pack].puzzles[current.level + i].opened = true
        } else {
            // Open next pack if it exists and is locked
            if (current.pack != packIds.size - 1 && !packs[current.pack + 1].opened) {
                packs[current.pack + 1].opened = true
                menu?.findItem(packIds[current.pack + 1])?.icon =
                        ContextCompat.getDrawable(this, R.drawable.ic_lock_open)
            }

            if (current.level != packSize - 1) {
                // Open lead-off levels until end of pack
                for (i in (current.level + 1)..(packSize - 1))
                    packs[current.pack].puzzles[i].opened = true

                // Open rest levels in next pack if possible
                if (current.pack != packIds.size - 1)
                    for (i in 0 until packSize - 1 - current.level)
                        packs[current.pack + 1].puzzles[i].opened = true
            } else {
                // Open levels only in next pack if possible
                if (current.pack != packIds.size - 1)
                    for (i in 0 until levelOpeningDelta)
                        packs[current.pack + 1].puzzles[i].opened = true
            }
        }
    }

    private fun selectNextLevel() {
        if (current.level != packSize - 1 || current.pack != packIds.size - 1) {
            // Open next direct level
            if (current.level < packSize - 1)
                current.level++
            else {
                current.level = 0
                current.pack++
            }
        }
    }

    private fun getGridCoordsFromTouch(ev: MotionEvent): IntArray {
        for (i in 0 until grid!!.childCount) {
            val primitive = grid?.getChildAt(i) as Button
            val location = IntArray(2)

            primitive.getLocationOnScreen(location)

            if (ev.rawX > location[0] && ev.rawX < location[0] + primitiveSize &&
                ev.rawY > location[1] && ev.rawY < location[1] + primitiveSize
            )
                return primitive.tag as IntArray
        }

        return intArrayOf(-1, -1)
    }

    private fun paintPrimitive(coords: IntArray) {
        val primitive: Button = grid!!.findViewWithTag(coords)

        val background = primitive.background as GradientDrawable
        background.setColor(if (eraseMode) Color.WHITE else colors!![paintColor])
        primitive.background = background

        puzzle!![coords[0], coords[1]] = if (eraseMode) "e" else paintColor.toString()
        savePartition()

        if (puzzle!!.checkIfSolved())
            handleSolvedPuzzle()
    }

    private val gridListener = { _: View, e: MotionEvent ->
        val coords = getGridCoordsFromTouch(e)

        if (!coords.contentEquals(intArrayOf(-1, -1)) &&
            puzzle!![coords[0], coords[1]] != 'b' && !current.levelSolved) {

            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    previousPaintCoords = coords.copyOf()

                    eraseMode = (puzzle!![coords[0], coords[1]].toInt() - 48) == paintColor

                    paintPrimitive(coords)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!coords.contentEquals(previousPaintCoords)) {
                        previousPaintCoords = coords.copyOf()

                        if (!eraseMode || puzzle!![coords[0], coords[1]].toInt() - 48 == paintColor)
                            paintPrimitive(coords)
                    }
                }
                else -> Log.d("ERROR", "Incorrect touch move occured!")
            }
        }
        true
    }

    private val colorPaletteListener = { v: View ->
        if (!current.levelSolved && v.tag != "paletteButton_" + paintColor.toString()) {
            // Set edit icon on pressed palette button
            palette?.findViewWithTag<ImageButton>(v.tag)?.setImageDrawable(
                ContextCompat.getDrawable(this, R.drawable.ic_edit)
            )
            // Remove edit icon from previous palette button
            palette?.findViewWithTag<ImageButton>("paletteButton_" + paintColor.toString())
                ?.setImageResource(android.R.color.transparent)

            // Update paint color
            paintColor = v.tag.toString().takeLast(1).toInt()
            savePaletteStatus()
        }
    }

    private val fabListener = { v: View ->
        if (!fabIsLocked)
            if (current.level == packSize - 1 && current.pack == packIds.size - 1)
                launchMailApplication()
            else {
                fabIsLocked = true
                (v as FloatingActionButton).hide()
                saveFabStatus(false)

                grid?.removeAllViews()
                palette?.removeAllViews()

                current.levelSolved = false

                selectNextLevel()
                saveCurrentSelectedLevel()

                packs[current.pack].puzzles[current.level].refresh()
                savePartition()

                paintColor = packs[current.pack].puzzles[current.level].parts / 2
                savePaletteStatus()

                onLayoutLoad()
            }
    }

    private val onShownFabListener = object : FloatingActionButton.OnVisibilityChangedListener() {
        override fun onShown(fab: FloatingActionButton?) {
            super.onShown(fab)
            fabIsLocked = false

            if (current.level == packSize - 1 && current.pack == packIds.size - 1) {
                fab?.setImageDrawable(ContextCompat.getDrawable(applicationContext, R.drawable.ic_mail_fab))
                fabIconChanged = true
            }
        }
    }
}
