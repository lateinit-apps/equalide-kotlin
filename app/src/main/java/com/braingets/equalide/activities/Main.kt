package com.braingets.equalide.activities

import java.io.File
import java.io.FileInputStream

import android.Manifest
import android.net.Uri
import android.util.Log
import android.os.Bundle

import android.content.Intent
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager

import android.support.v4.app.ActivityCompat
import android.support.v4.view.GravityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.ActionBarDrawerToggle
import android.support.design.widget.Snackbar
import android.support.design.widget.NavigationView
import android.support.design.widget.FloatingActionButton

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

import kotlinx.android.synthetic.main.main_screen.*
import kotlinx.android.synthetic.main.activity_main.*

import com.braingets.equalide.R
import com.braingets.equalide.data.Directory
import com.braingets.equalide.data.LevelData
import com.braingets.equalide.logic.Pack
import com.braingets.equalide.logic.Puzzle

const val READ_PERMISSION_REQUEST = 1

class Main : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    // Different element sizes
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
    private var directory: Directory? = null
    private var selectedPackInNav: Int = 0

    //!!!
    private val packIds = arrayOf(
        R.id.pack_01, R.id.pack_02, R.id.pack_03,
        R.id.pack_04, R.id.pack_05, R.id.pack_06,
        R.id.pack_07, R.id.pack_08
    )

    // Walkthrough related
    object Current {
        var level: Int = 0
        var pack: Int = 0
        var directory: Int = 0
        var levelSolved: Boolean = false
    }
    private val levelData = LevelData()
    private val levelOpeningDelta: Int = 3
    private var skipSolvedLevels: Boolean = true

    // Views related
    private var menu: Menu? = null
    private var toast: Toast? = null
    private var snackbar: Snackbar? = null
    private var fabIsShowed: Boolean = false
    private var fabIsLocked: Boolean = false
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

            // Hide toast message if opening navigation drawer
            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                super.onDrawerSlide(drawerView, slideOffset)
                toast?.cancel()
                snackbar?.dismiss()
            }
        }
        activity_main.addDrawerListener(toggle)
        toggle.syncState()

        // Find or create views
        grid = findViewById(R.id.puzzle_grid)
        palette = findViewById(R.id.color_palette)
        menu = findViewById<NavigationView>(R.id.nav_view).menu
        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
        snackbar = Snackbar.make(findViewById(R.id.content_view),
            R.string.snackbar_message, Snackbar.LENGTH_INDEFINITE)
        snackbar?.setAction(R.string.snackbar_action) { launchMailApplication() }

        // Add listeners to views
        grid?.setOnTouchListener(gridListener)
        nav_view.setNavigationItemSelectedListener(this)
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener(fabListener)

        // Handle opening file intent
        if (intent.action == Intent.ACTION_VIEW) {

            // Check for read permission
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_PERMISSION_REQUEST)
            else {
                val file = File(intent.data.path)
                val content = FileInputStream(file).bufferedReader().use { it.readText() }
                loadNewDirectory(content)
            }
        }

        if (directory != null) {
            loadDefaultDirectory()
            loadLevelData()
            loadUserData()

            // Listener to detect when layout is loaded to get it's resolution properties
            grid?.viewTreeObserver?.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    grid?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                    onLayoutLoad()
                }
            })
        }
    }

    // Close drawer on return from select level activity
    override fun onResume() {
        super.onResume()
        if (navigatedToSelectScreen && activity_main.isDrawerOpen(GravityCompat.START)) {
            navigatedToSelectScreen = false
            activity_main.closeDrawer(GravityCompat.START, false)
        }
    }

    // Close application if read permissions isn't granted
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == READ_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() && grantResults[0] != PackageManager.PERMISSION_GRANTED)
            this.finishAffinity()
    }

    // Launch new activity if selected proper item from navigation drawer
    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.send_mail)
            launchMailApplication()
        else {
            for (i in 0 until packIds.size)
                if (item.itemId == packIds[i]) {
                    // Fixes issue when selected item becomes unselectable
                    // on first select after closing of navigation drawer
                    menu?.getItem(i)?.isChecked = true

                    // Set color for text
                    val spanString = SpannableString(item.title.toString())
                    spanString.setSpan(ForegroundColorSpan(ContextCompat.getColor(
                        this, R.color.nav_text_selected)), 0,
                        spanString.length, 0)
                    item.title = spanString

                    selectedPackInNav = i
                    break
                }
            if (directory!![selectedPackInNav].opened)
                launchSelectLevelActivity()
        }
        return true
    }

    private fun launchMailApplication() {
        val intent = Intent(Intent.ACTION_SENDTO)
        intent.data = Uri.parse("mailto:${resources
            .getString(R.string.contact_mail_address)}")
        intent.putExtra(Intent.EXTRA_SUBJECT, "Equalide")

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

        for (level in directory!![selectedPackInNav].puzzles)
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

            Current.level = selectedLevel
            Current.pack = selectedPackInNav
            Current.levelSolved = false
            saveCurrentSelectedLevel()

            paintColor = directory!![Current.pack].puzzles[Current.level].getAmountOfParts() / 2
            savePaletteStatus()

            directory!![Current.pack].puzzles[Current.level].refresh()

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
        if (item.itemId == R.id.refresh_button) {
            refreshGrid()
            snackbar?.dismiss()
        }
        else
            return super.onOptionsItemSelected(item)

        return true
    }

    fun onLayoutLoad() {
        supportActionBar?.title = "Equalide   ${Current.pack + 1}-${(Current.level + 1)
            .toString().padStart(2, '0')}"
        calculateViewsSizes()

        puzzle = directory!![Current.pack].puzzles[Current.level]
        colors = resources.getIntArray(resources.getIdentifier(
                "colors_for_${puzzle!!.getAmountOfParts()}_parts",
                "array", this.packageName))

        addColorPalette(colors!!)

        // Happens when app reloads on solved level state
        if (fabIsShowed)
            hideColorPalette()

        renderPuzzle(puzzle!!)
    }

    private fun loadDefaultDirectory() {
        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        val packsAmount = preferences.getInt("Default directory size", 0)

        if (packsAmount > 0)
            loadDirectoryContent(preferences, packsAmount, -1, "Default")
        else
            levelData.addDirectory(mutableListOf(), "Default")
    }

    private fun loadLevelData() {
        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        val directoriesSizes = preferences.getString("Directories sizes", null)

        if (directoriesSizes != null) {
            val sizes = directoriesSizes.split("\n")

            for (directoryIndex in 0 until sizes.size - 1) {
                val name = preferences.getString("Directory [$directoryIndex] name", null)
                loadDirectoryContent(preferences, sizes[directoryIndex].toInt(), directoryIndex, name)
            }
        }
    }

    private fun loadDirectoryContent(preferences: SharedPreferences, packsAmount: Int,
                                     directoryIndex: Int, directoryName: String) {
        val directory = mutableListOf<Pack>()

        for (packIndex in 0 until packsAmount) {
            val packRaw =
                preferences.getString("Directory [$directoryIndex] Pack [$packIndex]", null)

            if (packRaw != null) {
                val packParsed = packRaw.split("\n\n")
                directory.add(Pack(Array(packParsed.size) { i -> Puzzle(packParsed[i]) }))
            }
        }

        levelData.addDirectory(directory, directoryName)
    }

    private fun loadNewDirectory(text: String) {
        Log.i("TAG", text)
    }

    private fun loadUserData() {
        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        // Get user progress
//        val packProgress = "sssssso"
//        val levelProgress = ("s".repeat(24) + "\n").repeat(6) + "s".repeat(23) + "o\n"
        val packProgress = preferences.getString("Pack progress", null)
        val levelProgress = preferences.getString("Level progress", null)
        val levelPartition = preferences.getString("Level partition", null)
        val currentPosition = preferences.getString("Current position", null)

        // Get state variables
        paintColor = preferences.getInt("Palette status", 0)
        fabIsShowed = preferences.getBoolean("Fab status", false)
        skipSolvedLevels = preferences.getBoolean("Skip status", true)

        if (packProgress == null || levelProgress == null) {
            // Open first pack and three levels if it's new game
            directory!![0].opened = true

            for (i in 0 until levelOpeningDelta)
                directory!![0].puzzles[i].opened = true
        } else {
            // Load pack progress and change icons in navigation drawer
            for (i in 0 until packProgress.length) {
                when (packProgress[i]) {
                    's' -> {
                        directory!![i].opened = true
                        directory!![i].solved = true
                        menu?.findItem(packIds[i])?.icon =
                                ContextCompat.getDrawable(this, R.drawable.ic_star)
                    }
                    'o' -> {
                        directory!![i].opened = true
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
                            directory!![i].puzzles[j].opened = true
                            directory!![i].puzzles[j].solved = true
                        }
                        'o' -> directory!![i].puzzles[j].opened = true
                    }
                }

            // Open 8 pack
            if (packProgress.length == 7 && packProgress[6] != 'c' && directory!![6].puzzles[21].solved) {
                directory!![7].opened = true
                menu?.findItem(packIds[7])?.icon =
                        ContextCompat.getDrawable(this, R.drawable.ic_lock_open)
                var forOpen = 0
                for (i in 21 until 24)
                    if (directory!![6].puzzles[i].solved)
                        forOpen++

                for (i in 0 until  forOpen)
                    directory!![7].puzzles[i].opened = true
            }
        }

        // Load current level
        if (currentPosition != null) {
            Current.pack = currentPosition.substring(0..0).toInt()
            Current.level = currentPosition.substring(1).toInt()
        }

        // Load current level partition
        if (levelPartition != null) {
            val puzzle = directory!![Current.pack].puzzles[Current.level]

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
            Current.levelSolved = true

            if (!checkIfAllLevelsSolved()
                || Current.level != directory!!.size - 1 || Current.pack != packIds.size - 1)
                findViewById<FloatingActionButton>(R.id.fab).show(onShownFabListener)
        }
    }

    private fun saveProgress() {
        var packProgress = ""
        var levelProgress = ""

        // Prepare pack progress
        for (pack in directory!!)
            packProgress += if (pack.solved) "s" else if (pack.opened) "o" else "c"

        // Prepare level progress
        for (pack in directory!!) {
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
        val levelPartition = directory!![Current.pack].puzzles[Current.level].getPartition()

        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        with(preferences.edit()) {
            putString("Level partition", levelPartition)
            apply()
        }
    }

    private fun saveCurrentSelectedLevel() {
        val currentPosition = Current.pack.toString() + Current.level.toString()

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

    private fun saveSkipStatus() {
        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        with(preferences.edit()) {
            putBoolean("Skip status", skipSolvedLevels)
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

            paletteButton.tag = "paletteButton_" + i.toString()

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

        for (i in 0 until puzzle.height)
            for (j in 0 until puzzle.width) {
                val primitive = Button(this)

                // Set size
                val params = LinearLayout.LayoutParams(primitiveSize, primitiveSize)
                primitive.layoutParams = params

                // Set color
                val drawable = ContextCompat.getDrawable(
                    this, R.drawable.primitive_border
                ) as GradientDrawable
                drawable.setColor(
                    when (puzzle[i, j]) {
                        'b' -> Color.BLACK
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
        skipSolvedLevels = !directory!![Current.pack].puzzles[Current.level].solved
        saveSkipStatus()

        hideColorPalette()

        // Is true for last level in game although no fab is actually shown.
        // It's because this setting is also used to save current level solved property.
        saveFabStatus(true)

        val allLevelsSolved = checkIfAllLevelsSolved()
        var packSolved = false
        Current.levelSolved = true

        if (!allLevelsSolved) {
            directory!![Current.pack].puzzles[Current.level].solved = true

            if (!directory!![Current.pack].solved && directory!![Current.pack].checkIfSolved()) {
                packSolved = true
                directory!![Current.pack].solved = true
                menu?.findItem(packIds[Current.pack])?.icon =
                        ContextCompat.getDrawable(this, R.drawable.ic_star)
            }

            openNextLevels()
            saveProgress()
        }

        if (Current.level != directory!!.size - 1 || Current.pack != packIds.size - 1) {
            if (packSolved) {
                toast?.setText("Pack ${Current.pack + 1} solved!")
                toast?.duration = Toast.LENGTH_LONG
            } else {
                toast?.setText("Puzzle solved!")
                toast?.duration = Toast.LENGTH_SHORT
            }
            toast?.show()
            findViewById<FloatingActionButton>(R.id.fab).show(onShownFabListener)
        } else if (!allLevelsSolved)
            snackbar?.show()
    }

    private fun refreshGrid() {
        puzzle?.refresh()
        savePartition()

        if (!Current.levelSolved) {
            for (i in 0 until grid!!.childCount) {
                val primitive = grid?.getChildAt(i) as Button
                val coords = primitive.tag as IntArray
                val background = primitive.background as GradientDrawable

                background.setColor(if (puzzle!![coords[0], coords[1]] == 'b') Color.BLACK else Color.WHITE)
                primitive.background = background
            }
        } else {
            toast?.cancel()

            grid?.removeAllViews()
            palette?.removeAllViews()

            findViewById<FloatingActionButton>(R.id.fab).hide()
            saveFabStatus(false)

            Current.levelSolved = false

            paintColor = directory!![Current.pack].puzzles[Current.level].getAmountOfParts() / 2
            savePaletteStatus()

            onLayoutLoad()
        }
    }

    private fun hideColorPalette() {
        for (i in 0 until palette!!.childCount)
            palette?.getChildAt(i)?.setBackgroundColor(Color.BLACK)
    }

    private fun checkIfAllLevelsSolved(): Boolean {
        for (pack in directory!!)
            if (!pack.solved)
                return false

        return true
    }

    private fun openNextLevels() {
        // Open levels in current pack
        if (Current.level <= directory!!.size - 1 - levelOpeningDelta) {
            for (i in 1..levelOpeningDelta)
                directory!![Current.pack].puzzles[Current.level + i].opened = true
        } else {
            // Open next pack if it exists and is locked
            if (Current.pack != packIds.size - 1 && !directory!![Current.pack + 1].opened) {
                directory!![Current.pack + 1].opened = true
                menu?.findItem(packIds[Current.pack + 1])?.icon =
                        ContextCompat.getDrawable(this, R.drawable.ic_lock_open)
            }

            if (Current.level != directory!!.size - 1) {
                // Open lead-off levels until end of pack
                for (i in (Current.level + 1)..(directory!!.size - 1))
                    directory!![Current.pack].puzzles[i].opened = true

                // Open rest levels in next pack if possible
                if (Current.pack != packIds.size - 1)
                    for (i in 0 until directory!!.size - 1 - Current.level)
                        directory!![Current.pack + 1].puzzles[i].opened = true
            } else {
                // Open levels only in next pack if possible
                if (Current.pack != packIds.size - 1)
                    for (i in 0 until levelOpeningDelta)
                        directory!![Current.pack + 1].puzzles[i].opened = true
            }
        }
    }

    private fun selectNextLevel() {
        if (!skipSolvedLevels &&
            (Current.level != directory!!.size - 1 || Current.pack != packIds.size - 1)
        ) {
            // Open next direct level
            if (Current.level < directory!!.size - 1)
                Current.level++
            else {
                Current.level = 0
                Current.pack++
            }
        } else {
            // Try to find next level in end of current pack
            for (i in Current.level + 1 until directory!!.size)
                if (directory!![Current.pack].puzzles[i].opened && !directory!![Current.pack].puzzles[i].solved) {
                    Current.level = i
                    return
                }

            // Try to find next level in all levels until last in game
            for (i in Current.pack + 1 until packIds.size)
                for (j in 0 until directory!!.size)
                    if (directory!![i].puzzles[j].opened && !directory!![i].puzzles[j].solved) {
                        Current.level = j
                        Current.pack = i
                        return
                    }

            // Try to find next level in all levels before current
            for (i in 0 until Current.pack)
                for (j in 0 until directory!!.size)
                    if (directory!![i].puzzles[j].opened && !directory!![i].puzzles[j].solved) {
                        Current.level = j
                        Current.pack = i
                        return
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
            puzzle!![coords[0], coords[1]] != 'b' && !Current.levelSolved) {

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
        if (v.tag != "paletteButton_" + paintColor.toString()) {
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
        if (!fabIsLocked) {
            fabIsLocked = true
            (v as FloatingActionButton).hide()
            saveFabStatus(false)

            toast?.cancel()

            grid?.removeAllViews()
            palette?.removeAllViews()

            Current.levelSolved = false

            selectNextLevel()
            saveCurrentSelectedLevel()

            directory!![Current.pack].puzzles[Current.level].refresh()
            savePartition()

            paintColor = directory!![Current.pack].puzzles[Current.level].getAmountOfParts() / 2
            savePaletteStatus()

            onLayoutLoad()
        }
    }

    private val onShownFabListener = object : FloatingActionButton.OnVisibilityChangedListener() {
        override fun onShown(fab: FloatingActionButton?) {
            super.onShown(fab)
            fabIsLocked = false
        }
    }
}
