package com.braingets.equalide.activities

import android.Manifest
import android.os.Bundle
import android.net.Uri
import android.util.Log

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
    private var fileUri: Uri? = null

    // Walkthrough related
    object CurrentPuzzle {
        var directory: Int = NO_LEVEL_OPENED
        var pack: Int = NO_LEVEL_OPENED
        var number: Int = NO_LEVEL_OPENED
        var solved: Boolean = false
    }
    private val levelData = LevelData()
    private val levelOpeningDelta: Int = 3
    private var skipSolvedLevels: Boolean = true

    // Views related
    private var menu: Menu? = null
    private var toast: Toast? = null
    private var snackbar: Snackbar? = null
    private var fab: FloatingActionButton? = null
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
        fab = findViewById(R.id.fab)
        
        // Add listeners to views
        grid?.setOnTouchListener(gridListener)
        nav_view.setNavigationItemSelectedListener(this)
        fab?.setOnClickListener(fabListener)

        loadLevelData()
        syncDirectoriesInNavigationDrawer()

        if (intent.action == Intent.ACTION_VIEW) {
            fileUri = intent.data
            onFileOpenIntent()
        }

        loadGameStatus()

        if (CurrentPuzzle.directory != NO_LEVEL_OPENED && CurrentPuzzle.pack != NO_LEVEL_OPENED
        && CurrentPuzzle.number != NO_LEVEL_OPENED) {
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
        when (item.itemId) {
            R.id.refresh_button -> {
                snackbar?.dismiss()

                if (CurrentPuzzle.directory != NO_LEVEL_OPENED && CurrentPuzzle.pack != NO_LEVEL_OPENED
                    && CurrentPuzzle.number != NO_LEVEL_OPENED)
                    refreshGrid()
            }
            R.id.solve_level_button -> {
                snackbar?.dismiss()

                if (CurrentPuzzle.directory != NO_LEVEL_OPENED && CurrentPuzzle.pack != NO_LEVEL_OPENED
                    && CurrentPuzzle.number != NO_LEVEL_OPENED) {

                    fab?.hide()
                    saveFabStatus(false)

                    levelData[CurrentPuzzle].loadSolution()
                    saveCurrentPartition()

                    grid?.removeAllViews()
                    renderPuzzle(levelData[CurrentPuzzle])

                    handleSolvedPuzzle()
                }
            }
            else ->
                return super.onOptionsItemSelected(item)
        }

        return true
    }

    // Launches new activity if selected proper item from navigation drawer
    override fun onNavigationItemSelected(item: MenuItem): Boolean {

        val selectedItemTitle = SpannableString(item.title.toString())

        if (item.itemId == R.id.unlock_anything) {
            selectedItemTitle.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, R.color.nav_text_selected)),
                0, selectedItemTitle.length, 0)
            item.title = selectedItemTitle

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
                        selectedItemTitle.setSpan(
                            ForegroundColorSpan(ContextCompat.getColor(this, R.color.nav_text_selected)),
                            0, selectedItemTitle.length, 0)
                        item.title = selectedItemTitle

                        navSelectedDirectory = null
                        navSelectedPackMenuItem = null
                        menu?.removeGroup(R.id.nav_menu_middle)
                    } else if (!navDeleteMode) {
                        val previousSelectedItemTitle = SpannableString(navSelectedDirectoryMenuItem?.title.toString())
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
                    if (navDeleteMode)
                        removeDirectory(i)
                    break
                }
        } else {
            selectedItemTitle.setSpan(
                ForegroundColorSpan(ContextCompat.getColor(this, R.color.nav_text_selected)),
                0, selectedItemTitle.length, 0)
            item.title = selectedItemTitle

            if (!navDeleteMode) {
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
        }
        return true
    }

    // Parse data received from select level activity
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        if (intent?.action == Intent.ACTION_VIEW) {
            fileUri = intent.data
            onFileOpenIntent()
        }
        else {
            val selectedLevel = intent?.getIntExtra("selected level", -1)

            if (selectedLevel != null && selectedLevel != -1) {
                grid?.removeAllViews()
                palette?.removeAllViews()

                if (fabIsShowed) {
                    fab?.hide()
                    saveFabStatus(false)
                }

                CurrentPuzzle.directory = navSelectedDirectory!!
                CurrentPuzzle.pack = navSelectedPack!!
                CurrentPuzzle.number = selectedLevel
                CurrentPuzzle.solved = false
                saveWalkthroughPosition()

                paintColor = levelData[CurrentPuzzle].getAmountOfParts() / 2
                savePaletteStatus()

                levelData[CurrentPuzzle].refresh()

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
        val content = contentResolver.openInputStream(fileUri).bufferedReader().use { it.readText() }
        loadLevelDataFrom(content)
        syncDirectoriesInNavigationDrawer()
    }

    fun onLayoutLoad() {
        val packName = if (CurrentPuzzle.directory == DEFAULT_DIRECTORY_INDEX &&
            CurrentPuzzle.pack == DEFAULT_PACK_INDEX) "Default" else "Pack ${CurrentPuzzle.pack + 1}"

        supportActionBar?.title = "${levelData[CurrentPuzzle.directory].name}/" +
                "$packName/${(CurrentPuzzle.number + 1).toString()
                    .padStart(levelData[CurrentPuzzle.directory][CurrentPuzzle.pack].size.toString().length, '0')}"

        calculateViewsSizes()

        colors = resources.getIntArray(resources.getIdentifier(
                "colors_for_${levelData[CurrentPuzzle].getAmountOfParts()}_parts",
                "array", this.packageName))

        addColorPalette(colors!!)

        // Happens when app reloads on solved level state
        if (fabIsShowed)
            hideColorPalette()

        renderPuzzle(levelData[CurrentPuzzle])
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
                levelData[DEFAULT_DIRECTORY_INDEX][DEFAULT_PACK_INDEX].addAll(MutableList(puzzles.size) { i -> Puzzle(puzzles[i].trimEnd()) })
                saveDirectoryData(levelData[DEFAULT_DIRECTORY_INDEX], true)
            } else {
                val directory = if (toDefaultDirectory) levelData[0] else Directory(packs[0].trim())

                for (packIndex in 1 until packs.size) {
                    val packParsed = packs[packIndex].split("\n\n")
                    directory.add(Pack(MutableList(packParsed.size) { i -> Puzzle(packParsed[i].trimEnd()) }))
                }

                if (!toDefaultDirectory)
                    levelData.add(directory)

                saveDirectoryData(directory, toDefaultDirectory)
            }
        }
        saveDirectoriesHashes()
    }

    private fun loadDirectoryData(preferences: SharedPreferences, directoryName: String, directoryHash: String,
                                  default: Boolean = false) {
        val directory = Directory(directoryName, if (default) DEFAULT_DIRECTORY_INDEX else directoryHash.toInt())
        val packHashesRaw = preferences.getString("Directory [$directoryHash] hashes", null)
        val packProgress = preferences.getString("Directory [$directoryHash] progress", null)

        if (packHashesRaw != null && packProgress != null) {
            val packHashes = packHashesRaw.split("\n")

            for (packIndex in 0 until packHashes.size) {
                val packRaw =
                    preferences.getString("Directory [$directoryHash] Pack [${packHashes[packIndex]}]", null)

                if (packRaw != null) {
                    val packParsed = packRaw.split("\n\n")
                    directory.add(
                        Pack(MutableList(packParsed.size) { i -> Puzzle(packParsed[i]) }),
                        packHashes[packIndex].toInt()
                    )
                } else
                    directory.add(Pack(mutableListOf()))

                when (packProgress[packIndex]) {
                    's' -> {
                        directory[packIndex].opened = true
                        directory[packIndex].solved = true
                    }
                    'o' -> directory[packIndex].opened = true
                }

                val levelProgress =
                    preferences.getString("Directory [$directoryHash] Pack [${packHashes[packIndex]}] progress", null)

                if (levelProgress != null)
                    for (levelIndex in 0 until levelProgress.length)
                        when (levelProgress[levelIndex]) {
                            's' -> {
                                directory[packIndex][levelIndex].opened = true
                                directory[packIndex][levelIndex].solved = true
                            }
                            'o' -> directory[packIndex][levelIndex].opened = true
                        }
            }
        } else if (default) {
            directory.add(Pack(mutableListOf()))
            directory[0].opened = true

            with(preferences.edit()) {
                putString("Directory [Default] progress", "o")
                putString("Directory [Default] hashes", "$DEFAULT_PACK_INDEX")
                apply()
            }
        }

        levelData.add(directory)
    }

    private fun loadGameStatus() {
        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        val levelPartition = preferences.getString("level partition", null)
        val walkthroughPosition = preferences.getString("walkthrough position", null)

        // Get state variables
        paintColor = preferences.getInt("palette status", 0)
        fabIsShowed = preferences.getBoolean("fab status", false)
        skipSolvedLevels = preferences.getBoolean("skip status", true)

        // Load current level
        if (walkthroughPosition != null) {
            val position = walkthroughPosition.split("\n")

            CurrentPuzzle.directory = position[0].toInt()
            CurrentPuzzle.pack = position[1].toInt()
            CurrentPuzzle.number = position[2].toInt()
        }

        // Load current level partition
        if (levelPartition != null) {
            val puzzle = levelData[CurrentPuzzle]

            if (puzzle.width * puzzle.height == levelPartition.length &&
                    !levelPartition.any { c: Char -> !c.isDigit() && c != 'e' && c != 'b' })
                // Successful case
                puzzle.setPartition(levelPartition)
            else {
                // Data is corrupted, loads clean partition instead
                puzzle.refresh()
                Log.d("error", "Incorrect load ocurred!")
            }
        }

        // Show fab if exited on opened fab
        if (fabIsShowed) {
            CurrentPuzzle.solved = true

            if (!checkIfAllLevelsSolved()
                || CurrentPuzzle.number != levelData[CurrentPuzzle.directory][CurrentPuzzle.pack].size - 1
                || CurrentPuzzle.pack != levelData[CurrentPuzzle.directory].size - 1)
                fab?.show(onShownFabListener)
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

    private fun saveDirectoryData(directory: Directory, default: Boolean = false) {

        val directoryProgress = Array(directory.size) { i -> if (directory[i].solved) 's' else
            if (directory[i].opened) 'o' else 'c' }.joinToString("")

        val packHashes = Array(directory.size) { i -> directory.getPackId(i).toString() }
        val packHashesRaw = packHashes.joinToString("\n")
        val packs = Array(directory.size)
            { i -> Array(directory[i].size)
                { j -> directory[i][j].getSource() }.joinToString("\n\n") }
        val packsProgress = Array(directory.size)
            { i -> Array(directory[i].size)
                { j -> if (directory[i][j].solved) 's' else
                    if (directory[i][j].opened) 'o' else 'c' }.joinToString("") }

        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        
        // Save new directory level data
        with(preferences.edit()) {
            if (!default)
                putString("Directory [${directory.id}] name", directory.name)

            val directoryHash = if (default) "Default" else directory.id.toString()
            putString("Directory [$directoryHash] progress", directoryProgress)
            putString("Directory [$directoryHash] hashes", packHashesRaw)

            for (packIndex in 0 until directory.size) {
                putString("Directory [$directoryHash] Pack [${packHashes[packIndex]}]", packs[packIndex])
                putString("Directory [$directoryHash] Pack [${packHashes[packIndex]}] progress", packsProgress[packIndex])
            }

            apply()
        }
    }

    private fun savePackProgress(pack: Pack, packHash: Int, directoryHash: String) {
        val packProgress = Array(pack.size)
            { i -> if (pack[i].solved) 's' else
                if (pack[i].opened) 'o' else 'c' }.joinToString("")

        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        
        with(preferences.edit()) {
            putString("Directory [$directoryHash] Pack [$packHash] progress", packProgress)
            apply()
        }
    }

    private fun saveDirectoryProgress(directory: Directory, default: Boolean = true) {
        val directoryProgress = Array(directory.size) { i -> if (directory[i].solved) 's' else
            if (directory[i].opened) 'o' else 'c' }.joinToString("")

        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        
        with(preferences.edit()) {
            putString("Directory [${if (default) "Default" else directory.id.toString()}] progress", directoryProgress)
            apply()
        }
    }

    private fun saveCurrentPartition() {
        val levelPartition = levelData[CurrentPuzzle].getPartition()

        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        with(preferences.edit()) {
            putString("level partition", levelPartition)
            apply()
        }
    }

    private fun saveWalkthroughPosition() {
        val position = "${CurrentPuzzle.directory}\n${CurrentPuzzle.pack}\n${CurrentPuzzle.number}"

        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        with(preferences.edit()) {
            putString("walkthrough position", position)
            apply()
        }
    }

    private fun saveFabStatus(status: Boolean) {
        fabIsShowed = status

        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        with(preferences.edit()) {
            putBoolean("fab status", status)
            apply()
        }
    }

    private fun savePaletteStatus() {
        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        with(preferences.edit()) {
            putInt("palette status", paintColor)
            apply()
        }
    }

    private fun saveSkipStatus() {
        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        with(preferences.edit()) {
            putBoolean("skip status", skipSolvedLevels)
            apply()
        }
    }

    private fun removeDirectory(index: Int) {
        val directoryHash = if (index == DEFAULT_DIRECTORY_INDEX) "Default" else levelData[index].id.toString()

        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        with(preferences.edit()) {
            remove("Directory [$directoryHash] hashes")
            remove("Directory [$directoryHash] progress")

            for (packIndex in 0 until levelData[index].size) {
                remove("Directory [$directoryHash] Pack [${levelData[index].getPackId(packIndex)}]")
                remove("Directory [$directoryHash] Pack [${levelData[index].getPackId(packIndex)}] progress")
            }

            apply()
        }

        if (index == DEFAULT_DIRECTORY_INDEX)
            levelData[DEFAULT_DIRECTORY_INDEX].clear(true)
        else {
            levelData.removeDirectory(index)
            saveDirectoriesHashes()

            if (navSelectedDirectory != null && index < navSelectedDirectory!!)
                navSelectedDirectory = navSelectedDirectory!! - 1
        }


        if (index == CurrentPuzzle.directory) {
            supportActionBar?.title = getString(R.string.app_name)

            toast?.cancel()

            grid?.removeAllViews()
            palette?.removeAllViews()

            fab?.hide()
            
            CurrentPuzzle.directory = NO_LEVEL_OPENED
            CurrentPuzzle.pack = NO_LEVEL_OPENED
            CurrentPuzzle.number = NO_LEVEL_OPENED
            CurrentPuzzle.solved = false
            saveWalkthroughPosition()

            fabIsShowed = false
            paintColor = 0
            skipSolvedLevels = true

            with(preferences.edit()) {
                remove("level partition")
                remove("walkthrough position")
                remove("fab status")
                remove("palette status")
                remove("skip status")
                apply()
            }
        }

        syncDirectoriesInNavigationDrawer()
    }

    private fun openAllLevels() {
        var directoryIndex = 0

        for (directory in levelData) {
            var directoryChanged = false
            var packIndex = 0

            for (pack in directory) {
                var packChanged = false

                for (puzzle in pack)
                    if (!puzzle.opened) {
                        puzzle.opened = true
                        packChanged = true
                    }

                if (packChanged)
                    savePackProgress(pack, directory.getPackId(packIndex),
                        if (directoryIndex == DEFAULT_DIRECTORY_INDEX) "Default" else directory.id.toString())

                if (!pack.opened) {
                    pack.opened = true
                    directoryChanged = true
                }

                packIndex++
            }

            if (directoryChanged)
                saveDirectoryProgress(directory, directoryIndex == DEFAULT_DIRECTORY_INDEX)

            directoryIndex++
        }

        if (navSelectedDirectory != null)
            syncPacksInNavigationDrawer(levelData[navSelectedDirectory!!], navSelectedDirectory!! == DEFAULT_DIRECTORY_INDEX)
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
        skipSolvedLevels = !levelData[CurrentPuzzle].solved
        saveSkipStatus()

        hideColorPalette()

        // Is true for last level in game although no fab is actually shown.
        // It's because this setting is also used to save current level solved property.
        saveFabStatus(true)

        val allLevelsSolved = checkIfAllLevelsSolved()
        var packSolved = false
        CurrentPuzzle.solved = true

        if (!allLevelsSolved) {
            levelData[CurrentPuzzle].solved = true

            if (!levelData[CurrentPuzzle.directory][CurrentPuzzle.pack].solved
                && levelData[CurrentPuzzle.directory][CurrentPuzzle.pack].checkIfSolved()) {
                packSolved = true
                levelData[CurrentPuzzle.directory][CurrentPuzzle.pack].solved = true
                saveDirectoryProgress(levelData[CurrentPuzzle.directory], CurrentPuzzle.directory == DEFAULT_DIRECTORY_INDEX)

                if (navSelectedDirectory != null && navSelectedDirectory!! == CurrentPuzzle.directory)
                    syncPacksInNavigationDrawer(levelData[CurrentPuzzle.directory], CurrentPuzzle.directory == DEFAULT_DIRECTORY_INDEX)
            }

            openNextLevels()
            savePackProgress(
                levelData[CurrentPuzzle.directory][CurrentPuzzle.pack],
                levelData[CurrentPuzzle.directory].getPackId(CurrentPuzzle.pack),
                if (CurrentPuzzle.directory == DEFAULT_DIRECTORY_INDEX) "Default"
                else levelData[CurrentPuzzle.directory].id.toString())
        }

        if (CurrentPuzzle.number != levelData[CurrentPuzzle.directory][CurrentPuzzle.pack].size - 1
            || CurrentPuzzle.pack != levelData[CurrentPuzzle.directory].size - 1) {
            if (packSolved) {
                toast?.setText("Pack ${CurrentPuzzle.pack + 1} solved!")
                toast?.duration = Toast.LENGTH_LONG
            } else {
                toast?.setText("Puzzle solved!")
                toast?.duration = Toast.LENGTH_SHORT
            }
            toast?.show()
            fab?.show(onShownFabListener)
        } else if (!allLevelsSolved)
            snackbar?.show()
    }

    private fun refreshGrid() {
        levelData[CurrentPuzzle].refresh()
        saveCurrentPartition()

        if (!CurrentPuzzle.solved) {
            for (i in 0 until grid!!.childCount) {
                val primitive = grid?.getChildAt(i) as Button
                val coords = primitive.tag as IntArray
                val background = primitive.background as GradientDrawable

                background.setColor(if (levelData[CurrentPuzzle][coords[0], coords[1]] == 'b') Color.BLACK else Color.WHITE)
                primitive.background = background
            }
        } else {
            toast?.cancel()

            grid?.removeAllViews()
            palette?.removeAllViews()

            fab?.hide()
            saveFabStatus(false)

            CurrentPuzzle.solved = false

            paintColor = levelData[CurrentPuzzle].getAmountOfParts() / 2
            savePaletteStatus()

            onLayoutLoad()
        }
    }

    private fun hideColorPalette() {
        for (i in 0 until palette!!.childCount)
            palette?.getChildAt(i)?.setBackgroundColor(Color.BLACK)
    }

    private fun checkIfAllLevelsSolved(): Boolean {
        for (pack in levelData[CurrentPuzzle.directory])
            if (!pack.solved)
                return false

        return true
    }

    private fun openNextLevels() {
        // Open levels in current pack
        if (CurrentPuzzle.number <= levelData[CurrentPuzzle.directory][CurrentPuzzle.pack].size - 1 - levelOpeningDelta) {

            for (i in 1..levelOpeningDelta)
                levelData[CurrentPuzzle.directory][CurrentPuzzle.pack][CurrentPuzzle.number + i].opened = true
        } else {
            // Open next pack if it exists and is locked
            if (CurrentPuzzle.pack != levelData[CurrentPuzzle.directory].size - 1
                && !levelData[CurrentPuzzle.directory][CurrentPuzzle.pack + 1].opened) {

                levelData[CurrentPuzzle.directory][CurrentPuzzle.pack + 1].opened = true
                saveDirectoryProgress(levelData[CurrentPuzzle.directory])

                if (navSelectedDirectory != null && navSelectedDirectory!! == CurrentPuzzle.directory)
                    syncPacksInNavigationDrawer(levelData[CurrentPuzzle.directory], CurrentPuzzle.directory == DEFAULT_DIRECTORY_INDEX)
            }

            if (CurrentPuzzle.number != levelData[CurrentPuzzle.directory][CurrentPuzzle.pack].size - 1) {
                // Open lead-off levels until end of pack
                for (i in (CurrentPuzzle.number + 1)..(levelData[CurrentPuzzle.directory][CurrentPuzzle.pack].size - 1))
                    levelData[CurrentPuzzle.directory][CurrentPuzzle.pack][i].opened = true

                // Open rest levels in next pack if possible
                if (CurrentPuzzle.pack != levelData[CurrentPuzzle.directory].size - 1) {

                    var restLevels = levelData[CurrentPuzzle.directory][CurrentPuzzle.pack].size - 1 - CurrentPuzzle.number
                    if (restLevels > levelData[CurrentPuzzle.directory][CurrentPuzzle.pack + 1].size)
                        restLevels = levelData[CurrentPuzzle.directory][CurrentPuzzle.pack + 1].size

                    for (i in 0 until restLevels)
                        levelData[CurrentPuzzle.directory][CurrentPuzzle.pack + 1][i].opened = true

                    savePackProgress(
                        levelData[CurrentPuzzle.directory][CurrentPuzzle.pack + 1],
                        levelData[CurrentPuzzle.directory].getPackId(CurrentPuzzle.pack + 1),
                        if (CurrentPuzzle.directory == DEFAULT_DIRECTORY_INDEX) "Default"
                        else levelData[CurrentPuzzle.directory].id.toString())
                }
            } else {
                // Open levels only in next pack if possible
                if (CurrentPuzzle.pack != levelData[CurrentPuzzle.directory].size - 1) {
                    for (i in 0 until
                            if (levelData[CurrentPuzzle.directory][CurrentPuzzle.pack + 1].size >= levelOpeningDelta) levelOpeningDelta
                            else levelData[CurrentPuzzle.directory][CurrentPuzzle.pack + 1].size)
                        levelData[CurrentPuzzle.directory][CurrentPuzzle.pack + 1][i].opened = true

                    savePackProgress(
                        levelData[CurrentPuzzle.directory][CurrentPuzzle.pack + 1],
                        levelData[CurrentPuzzle.directory].getPackId(CurrentPuzzle.pack + 1),
                        if (CurrentPuzzle.directory == DEFAULT_DIRECTORY_INDEX) "Default"
                        else levelData[CurrentPuzzle.directory].id.toString())
                }
            }
        }
    }

    private fun selectNextLevel() {
        if (!skipSolvedLevels &&
            (CurrentPuzzle.number != levelData[CurrentPuzzle.directory][CurrentPuzzle.pack].size - 1
                    || CurrentPuzzle.pack != levelData[CurrentPuzzle.directory].size - 1)
        ) {
            // Open next direct level
            if (CurrentPuzzle.number < levelData[CurrentPuzzle.directory][CurrentPuzzle.pack].size - 1)
                CurrentPuzzle.number++
            else {
                CurrentPuzzle.number = 0
                CurrentPuzzle.pack++
            }
        } else {
            // Try to find next level in end of current pack
            for (i in CurrentPuzzle.number + 1 until levelData[CurrentPuzzle.directory][CurrentPuzzle.pack].size)
                if (levelData[CurrentPuzzle.directory][CurrentPuzzle.pack][i].opened
                    && !levelData[CurrentPuzzle.directory][CurrentPuzzle.pack][i].solved) {
                    CurrentPuzzle.number = i
                    return
                }

            // Try to find next level in all levels until last in game
            for (i in CurrentPuzzle.pack + 1 until levelData[CurrentPuzzle.directory].size)
                for (j in 0 until levelData[CurrentPuzzle.directory][i].size)
                    if (levelData[CurrentPuzzle.directory][i][j].opened && !levelData[CurrentPuzzle.directory][i][j].solved) {
                        CurrentPuzzle.number = j
                        CurrentPuzzle.pack = i
                        return
                    }

            // Try to find next level in all levels before current
            for (i in 0 until CurrentPuzzle.pack)
                for (j in 0 until levelData[CurrentPuzzle.directory][i].size)
                    if (levelData[CurrentPuzzle.directory][i][j].opened && !levelData[CurrentPuzzle.directory][i][j].solved) {
                        CurrentPuzzle.number = j
                        CurrentPuzzle.pack = i
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

        levelData[CurrentPuzzle][coords[0], coords[1]] = if (eraseMode) "e" else paintColor.toString()
        saveCurrentPartition()

        if (levelData[CurrentPuzzle].checkIfSolved())
            handleSolvedPuzzle()
    }

    private val gridListener = { _: View, e: MotionEvent ->
        val coords = getGridCoordsFromTouch(e)

        if (!coords.contentEquals(intArrayOf(-1, -1)) &&
            levelData[CurrentPuzzle][coords[0], coords[1]] != 'b' && !CurrentPuzzle.solved) {

            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    previousPaintCoords = coords.copyOf()

                    eraseMode = (levelData[CurrentPuzzle][coords[0], coords[1]].toInt() - 48) == paintColor

                    paintPrimitive(coords)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!coords.contentEquals(previousPaintCoords)) {
                        previousPaintCoords = coords.copyOf()

                        if (!eraseMode || levelData[CurrentPuzzle][coords[0], coords[1]].toInt() - 48 == paintColor)
                            paintPrimitive(coords)
                    }
                }
                else -> Log.d("error", "Incorrect touch move occured!")
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

            CurrentPuzzle.solved = false

            selectNextLevel()
            saveWalkthroughPosition()

            levelData[CurrentPuzzle].refresh()
            saveCurrentPartition()

            paintColor = levelData[CurrentPuzzle].getAmountOfParts() / 2
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
