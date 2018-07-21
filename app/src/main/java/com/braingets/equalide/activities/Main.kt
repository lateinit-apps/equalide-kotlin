package com.braingets.equalide.activities

import java.io.File
import java.io.FileInputStream

import android.Manifest
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
import com.braingets.equalide.logic.*
import com.braingets.equalide.data.LevelData

class Main : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    // Different element sizes
    object ContentView {
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
    private var filePath: String? = null

    //!!!
    private val packIds = arrayOf<Int>()

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
    private var navDeleteMode: Boolean = false
    private var navSelectedPack: Int? = null
    private var navSelectedPackMenuItem: MenuItem? = null
    private var navSelectedDirectory: Int? = null
    private var navSelectedDirectoryMenuItem: MenuItem? = null

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
                navSelectedPackMenuItem?.isChecked = false
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
        snackbar?.setAction(R.string.snackbar_action) {  }

        // Add listeners to views
        grid?.setOnTouchListener(gridListener)
        nav_view.setNavigationItemSelectedListener(this)
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener(fabListener)

        loadLevelData()
        syncDirectoriesInNavigationDrawer()

        if (intent.action == Intent.ACTION_VIEW) {
            filePath = intent.data.path
            onFileOpenIntent()
        }

        if (false) {
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
        else
            parseFileFromIntent()
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

    // Launches new activity if selected proper item from navigation drawer
    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        val selectedItemTitle = SpannableString(item.title.toString())

        if (item.itemId == R.id.unlock_anything) {
            openAllLevels()
            return true
        }

        if (item.itemId == R.id.delete_switch) {
            navDeleteMode = !navDeleteMode

            val color = if (navDeleteMode) R.color.red else R.color.nav_text_selected

            selectedItemTitle.setSpan(ForegroundColorSpan(ContextCompat.getColor(this, color)),
                0, selectedItemTitle.length, 0)
            item.title = selectedItemTitle
            return true
        }

        if (item.groupId == R.id.nav_menu_top) {
            for (i in 0 until levelData.size)
                if (item.itemId == levelData[i].id) {
                    if (i == navSelectedDirectory) {
                        selectedItemTitle.setSpan(ForegroundColorSpan(
                            ContextCompat.getColor(this, R.color.nav_text_selected)),
                            0, selectedItemTitle.length, 0)
                        item.title = selectedItemTitle
                        navSelectedDirectory = null
                        navSelectedPackMenuItem = null
                        menu?.removeGroup(R.id.nav_menu_middle)

                        if (navDeleteMode)
                            removeDirectory(i)
                    } else {
                        if (navDeleteMode)
                            removeDirectory(i)
                        else {
                            val previousSelectedItemTitle = SpannableString(
                                navSelectedDirectoryMenuItem?.title.toString())
                            previousSelectedItemTitle.setSpan(ForegroundColorSpan(
                                ContextCompat.getColor(this, R.color.nav_text_selected)),
                                0, previousSelectedItemTitle.length, 0)
                            navSelectedDirectoryMenuItem?.title = previousSelectedItemTitle

                            navSelectedDirectory = i
                            navSelectedDirectoryMenuItem = item
                            selectedItemTitle.setSpan(ForegroundColorSpan(
                                ContextCompat.getColor(this, R.color.green)),
                                0, selectedItemTitle.length, 0)
                            item.title = selectedItemTitle

                            syncPacksInNavigationDrawer(levelData[i], i == DEFAULT_DIRECTORY_INDEX)
                        }
                    }
                    break
                }
        } else {
            selectedItemTitle.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, R.color.nav_text_selected)),
                0, selectedItemTitle.length, 0)
            item.title = selectedItemTitle

            for (i in 0 until levelData[navSelectedDirectory!!].size)
                if (item.itemId == levelData[navSelectedDirectory!!].getPackId(i)) {
                    // Fixes issue when selected item becomes unselectable
                    // on first select after closing of navigation drawer
                    item.isChecked = true

                    navSelectedPack = i
                    navSelectedPackMenuItem = item
                    break
                }
            if (levelData[navSelectedDirectory!!][navSelectedPack!!].opened)
                launchSelectLevelActivity(levelData[navSelectedDirectory!!][navSelectedPack!!])
        }
        return true
    }

    // Parse data received from select level activity
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.action == Intent.ACTION_VIEW) {
            filePath = intent.data.path
            onFileOpenIntent()
        }
        else {
            val selectedLevel = intent?.getIntExtra("selected level", -1)

            if (selectedLevel != null && selectedLevel != -1) {
                grid?.removeAllViews()
                palette?.removeAllViews()

                if (fabIsShowed) {
                    findViewById<FloatingActionButton>(R.id.fab).hide()
                    saveFabStatus(false)
                }

                Current.level = selectedLevel
                Current.levelSolved = false
                saveCurrentSelectedLevel()

                paintColor = directory!![Current.pack][Current.level].getAmountOfParts() / 2
                savePaletteStatus()

                directory!![Current.pack][Current.level].refresh()

                onLayoutLoad()
            }
        }
    }

    private fun onFileOpenIntent() {
        // Check for read permission
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), READ_PERMISSION_REQUEST)
        else
            parseFileFromIntent()
    }

    private fun parseFileFromIntent() {
        val file = File(filePath)
        val content = FileInputStream(file).bufferedReader().use { it.readText() }
        loadLevelDataFrom(content)
        syncDirectoriesInNavigationDrawer()
    }

    fun onLayoutLoad() {
        supportActionBar?.title = "Equalide   ${Current.pack + 1}-${(Current.level + 1)
            .toString().padStart(2, '0')}"
        calculateViewsSizes()

        puzzle = directory!![Current.pack][Current.level]
        colors = resources.getIntArray(resources.getIdentifier(
                "colors_for_${puzzle!!.getAmountOfParts()}_parts",
                "array", this.packageName))

        addColorPalette(colors!!)

        // Happens when app reloads on solved level state
        if (fabIsShowed)
            hideColorPalette()

        renderPuzzle(puzzle!!)
    }

    private fun launchSelectLevelActivity(pack: Pack) {
        // String that contains user progress in selected pack
        // 's' - solved level
        // 'o' - opened level
        // 'c' - closed level
        var packLevelData = ""

        for (level in pack)
            packLevelData += if (level.solved) "s" else if (level.opened) "o" else "c"

        val packName = if (navSelectedDirectory!! == DEFAULT_DIRECTORY_INDEX &&
                navSelectedPack!! == DEFAULT_PACK_INDEX) "Default" else "Pack ${navSelectedPack!! + 1}"

        val intent = Intent(this, SelectLevel::class.java).apply {
            putExtra("pack", packName)
            putExtra("directory", levelData[navSelectedDirectory!!].name)
            putExtra("level data", packLevelData)
        }

        navigatedToSelectScreen = true
        startActivity(intent)

        overridePendingTransition(R.anim.left_right_enter, R.anim.left_right_exit)
    }

    private fun loadLevelData() {
        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        loadDirectoryData(preferences, "Default", "Default", true)

        val directoriesHashes = preferences.getString("Directories hashes", null)

        if (directoriesHashes != null) {
            //Log.i("tag", "\nLoaded dir hashes: ${directoriesHashes.replace("\n", "|")}")

            val hashes = directoriesHashes.split("\n")
            
            for (directoryIndex in 0 until hashes.size) {
                val name = preferences.getString("Directory [${hashes[directoryIndex]}] name", "Broken directory")
                loadDirectoryData(preferences, name, hashes[directoryIndex])
            }
        }
    }

    private fun loadLevelDataFrom(text: String) {
        val directories = text.split("##")

        val toDefaultDirectory = directories.size == 1
        val startIndex = if (toDefaultDirectory) 0 else 1

        for (directoryIndex in startIndex until directories.size) {
            val packs = directories[directoryIndex].split("\n#\n", "#\n")

            // To default pack in default directory
            if (packs.size == 1) {
                val puzzles = packs[0].split("\n\n")
                levelData[0][0].addAll(MutableList(puzzles.size) { i -> Puzzle(puzzles[i].trimEnd()) })
            } else {
                val directory = if (toDefaultDirectory) levelData[0] else Directory(packs[0].trim())

                for (packIndex in 1 until packs.size) {
                    val packParsed = packs[packIndex].split("\n\n")
                    directory.add(Pack(MutableList(packParsed.size) { i -> Puzzle(packParsed[i].trimEnd()) }))
                }

                if (!toDefaultDirectory)
                    levelData.add(directory)
                saveDirectoryData(directory)
            }
        }
        saveDirectoriesHashes()
    }

    private fun loadDirectoryData(preferences: SharedPreferences, directoryName: String, directoryHash: String,
                                  default: Boolean = false) {
        val directory = Directory(directoryName, if (default) DEFAULT_DIRECTORY_INDEX else directoryHash.toInt())
        val packHashesRaw = preferences.getString("Directory [$directoryHash] Pack hashes", null)

        if (packHashesRaw != null) {
            val packHashes = packHashesRaw.split("\n")

            for (packIndex in 0 until packHashes.size) {
                val packRaw =
                    preferences.getString("Directory [$directoryHash] Pack [${packHashes[packIndex]}", null)

                if (packRaw != null) {
                    val packParsed = packRaw.split("\n\n")
                    directory.add(Pack(MutableList(packParsed.size) { i -> Puzzle(packParsed[i]) }), packHashes[packIndex].toInt())
                }
            }
        } else if (default)
            directory.add(Pack(mutableListOf()))

        levelData.add(directory)
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
                directory!![0][i].opened = true
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
                            directory!![i][j].opened = true
                            directory!![i][j].solved = true
                        }
                        'o' -> directory!![i][j].opened = true
                    }
                }

            // Open 8 pack
            if (packProgress.length == 7 && packProgress[6] != 'c' && directory!![6][21].solved) {
                directory!![7].opened = true
                menu?.findItem(packIds[7])?.icon =
                        ContextCompat.getDrawable(this, R.drawable.ic_lock_open)
                var forOpen = 0
                for (i in 21 until 24)
                    if (directory!![6][i].solved)
                        forOpen++

                for (i in 0 until  forOpen)
                    directory!![7][i].opened = true
            }
        }

        // Load current level
        if (currentPosition != null) {
            Current.pack = currentPosition.substring(0..0).toInt()
            Current.level = currentPosition.substring(1).toInt()
        }

        // Load current level partition
        if (levelPartition != null) {
            val puzzle = directory!![Current.pack][Current.level]

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

    private fun saveDirectoriesHashes() {
        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        with(preferences.edit()) {
            if (levelData.size > 1) {
                val hashes = MutableList(levelData.size) { i -> levelData[i].id.toString() }

                // Remove default directory data
                hashes.removeAt(0)

                //Log.i("tag", "\nSync dir hashes: ${hashes.joinToString("|")}")
                putString("Directories hashes", hashes.joinToString("\n"))
            } else
                remove("Directories hashes")
            apply()
        }
    }

    private fun saveDirectoryData(directory: Directory) {
        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        val packHashes = MutableList(directory.size) { i -> directory.getPackId(i).toString() }
        val packHashesRaw = packHashes.joinToString("\n")
        val packs = MutableList(directory.size)
            { i -> MutableList(directory[i].size)
                { j -> directory[i][j].getRawSource() }.joinToString("\n\n") }

        //Log.i("tag", "\nNew directory hash: ${directory.id}")

        // Save new directory level data
        with(preferences.edit()) {
            putString("Directory [${directory.id}] name", directory.name)
            putString("Directory [${directory.id}] Pack hashes", packHashesRaw)

            for (packIndex in 0 until directory.size)
                putString("Directory [${directory.id}] Pack [${packHashes[packIndex]}", packs[packIndex])

            apply()
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
            for (level in pack)
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
        val levelPartition = directory!![Current.pack][Current.level].getPartition()

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

    private fun removeDirectory(index: Int) {
        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        //Log.i("tag", "\nRemoved directory hash: ${levelData[index].id}")

        if (index != DEFAULT_DIRECTORY_INDEX) {
            with(preferences.edit()) {
                for (packIndex in 0 until levelData[index].size)
                    remove("Directory [${levelData[index].id}] Pack [${levelData[index].getPackId(packIndex)}")

                apply()
            }

            levelData.removeDirectory(index)
            saveDirectoriesHashes()

            if (navSelectedDirectory != null && index < navSelectedDirectory!!)
                navSelectedDirectory = navSelectedDirectory!! - 1
        } else {
            // Remove default directory saved data
            with(preferences.edit()) {
                remove("Directory [Default] Pack hashes")

                for (packIndex in 0 until levelData[DEFAULT_DIRECTORY_INDEX].size)
                    remove("Directory [Default] Pack [${levelData[DEFAULT_DIRECTORY_INDEX].getPackId(packIndex)}")

                apply()
            }
            levelData[DEFAULT_DIRECTORY_INDEX].clear(true)
        }
        syncDirectoriesInNavigationDrawer()
    }

    private fun openAllLevels() {
        for (directory in levelData)
            for (pack in directory) {
                for (puzzle in pack)
                    if (!puzzle.opened) puzzle.opened = true
                if (!pack.opened) pack.opened = true
            }
    }

    private fun calculateViewsSizes() {
        val contentView = findViewById<LinearLayout>(R.id.content_view)

        colorPaletteSize = contentView.width / 5

        ContentView.width = contentView.width
        ContentView.height = (contentView.height
                - resources.getDimension(R.dimen.puzzle_grid_margin_top)
                - resources.getDimension(R.dimen.puzzle_grid_margin_bottom)
                - colorPaletteSize).toInt()
    }

    private fun syncDirectoriesInNavigationDrawer() {
        menu?.removeGroup(R.id.nav_menu_top)

        for (i in 0 until levelData.size)
            menu?.add(R.id.nav_menu_top, levelData[i].id, 0, levelData[i].name)

        menu?.setGroupCheckable(R.id.nav_menu_top, true, true)

        if (navSelectedDirectory != null) {
            navSelectedDirectoryMenuItem = menu?.getItem(navSelectedDirectory!!)

            val selectedItemTitle = SpannableString(navSelectedDirectoryMenuItem?.title.toString())
            selectedItemTitle.setSpan(ForegroundColorSpan(
                    ContextCompat.getColor(this, R.color.green)),
                0, selectedItemTitle.length, 0)
            navSelectedDirectoryMenuItem?.title = selectedItemTitle
        }
    }

    private fun syncPacksInNavigationDrawer(directory: Directory, default: Boolean = false) {
        menu?.removeGroup(R.id.nav_menu_middle)

        if (default)
            menu?.add(R.id.nav_menu_middle, directory.getPackId(0), 1, "Default")

        for (i in (if (default) 1 else 0) until directory.size) {
            val item = menu?.add(R.id.nav_menu_middle, directory.getPackId(i), 1,
                "Pack ${(i + 1).toString().padStart(directory.size.toString().length, '0')}")
            item?.icon = ContextCompat.getDrawable(this, if (directory[i].opened)
                if (directory[i].solved) R.drawable.ic_star else R.drawable.ic_lock_open else R.drawable.ic_lock)
        }

        menu?.setGroupCheckable(R.id.nav_menu_middle, true, true)
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
        primitiveSize = minOf(ContentView.width / puzzle.width, ContentView.height / puzzle.height)

        grid?.columnCount = puzzle.width
        grid?.rowCount = puzzle.height

        // Center-align puzzle between action bar and palette
        val gridParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT)
        gridParams.bottomMargin = (ContentView.height - puzzle.height * primitiveSize) / 2
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
        skipSolvedLevels = !directory!![Current.pack][Current.level].solved
        saveSkipStatus()

        hideColorPalette()

        // Is true for last level in game although no fab is actually shown.
        // It's because this setting is also used to save current level solved property.
        saveFabStatus(true)

        val allLevelsSolved = checkIfAllLevelsSolved()
        var packSolved = false
        Current.levelSolved = true

        if (!allLevelsSolved) {
            directory!![Current.pack][Current.level].solved = true

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

            paintColor = directory!![Current.pack][Current.level].getAmountOfParts() / 2
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
                directory!![Current.pack][Current.level + i].opened = true
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
                    directory!![Current.pack][i].opened = true

                // Open rest levels in next pack if possible
                if (Current.pack != packIds.size - 1)
                    for (i in 0 until directory!!.size - 1 - Current.level)
                        directory!![Current.pack + 1][i].opened = true
            } else {
                // Open levels only in next pack if possible
                if (Current.pack != packIds.size - 1)
                    for (i in 0 until levelOpeningDelta)
                        directory!![Current.pack + 1][i].opened = true
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
                if (directory!![Current.pack][i].opened && !directory!![Current.pack][i].solved) {
                    Current.level = i
                    return
                }

            // Try to find next level in all levels until last in game
            for (i in Current.pack + 1 until packIds.size)
                for (j in 0 until directory!!.size)
                    if (directory!![i][j].opened && !directory!![i][j].solved) {
                        Current.level = j
                        Current.pack = i
                        return
                    }

            // Try to find next level in all levels before current
            for (i in 0 until Current.pack)
                for (j in 0 until directory!!.size)
                    if (directory!![i][j].opened && !directory!![i][j].solved) {
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

            directory!![Current.pack][Current.level].refresh()
            savePartition()

            paintColor = directory!![Current.pack][Current.level].getAmountOfParts() / 2
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
