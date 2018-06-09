package equalide.kotlin.activities

import android.os.Bundle
import android.content.Context
import android.content.Intent
import android.support.v4.view.GravityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity
import android.support.v7.app.ActionBarDrawerToggle
import android.support.design.widget.NavigationView
import android.support.design.widget.FloatingActionButton
import android.util.Log
import android.view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.widget.*
import equalide.kotlin.R
import equalide.kotlin.logic.Pack
import equalide.kotlin.logic.Puzzle
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_screen.*
import java.io.InputStream
import android.content.pm.PackageManager

class Main : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    // Sizes
    private val gridArea = object {
        var width: Int = 0
        var height: Int = 0
    }
    private var colorPaletteSize: Int = 0
    private var primitiveSize: Int = 0

    // Draw related
    private var drawColor: Int = 0
    private var prevTouchCoords: IntArray? = null
    private var writeModeOn: Boolean = false
    private var colors: IntArray? = null

    // Puzzle related
    private var loadedPuzzle: Puzzle? = null
    private var packs: Array<Pack>? = null
    private val packSize: Int = 24
    private val packIds = arrayOf(
        R.id.pack_01, R.id.pack_02, R.id.pack_03,
        R.id.pack_04, R.id.pack_05, R.id.pack_06,
        R.id.pack_07
    )
    private var selectedPack: Int = 0

    // Walkthrough related
    private val current = object {
        var level: Int = 0
        var pack: Int = 0
        var levelSolved: Boolean = false
    }
    private val openDelta: Int = 3
    private var skipSolved: Boolean = true

    // Other
    private var menu: Menu? = null
    private var fabIsShowed: Boolean = false
    private var onSelectScreen: Boolean = false
    private var toast: Toast? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = object : ActionBarDrawerToggle(
            this, activity_main, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        ) {
            override fun onDrawerClosed(drawerView: View) {
                super.onDrawerClosed(drawerView)
                menu?.getItem(selectedPack)?.isChecked = false
            }

            override fun onDrawerSlide(drawerView: View, slideOffset: Float) {
                super.onDrawerSlide(drawerView, slideOffset)
                toast?.cancel()
            }
        }
        activity_main.addDrawerListener(toggle)

        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)
        menu = findViewById<NavigationView>(R.id.nav_view).menu

        packs = loadPacks()
        loadUserProgress()

        toast = Toast.makeText(this, "", Toast.LENGTH_SHORT)
        findViewById<FloatingActionButton>(R.id.fab).setOnClickListener(fabListener)

        val grid = findViewById<GridLayout>(R.id.puzzle_grid)
        grid.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                grid.viewTreeObserver.removeOnGlobalLayoutListener(this)
                onLayoutLoad()
            }
        })
    }

    override fun onResume() {
        super.onResume()
        if (onSelectScreen && activity_main.isDrawerOpen(GravityCompat.START)) {
            onSelectScreen = false
            activity_main.closeDrawer(GravityCompat.START, false)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val closeDrawer = intent?.getBooleanExtra("close drawer", false)
        if (closeDrawer != null) {}


        val selectedLevel = intent?.getStringExtra("selected level")?.toInt()
        if (selectedLevel != null) {
            refreshContentArea()
            if (fabIsShowed) {
                findViewById<FloatingActionButton>(R.id.fab).hide()
                saveFabStatus(false)
            }

            current.level = selectedLevel
            current.pack = selectedPack
            current.levelSolved = false
            packs!![current.pack].puzzles[current.level].refresh()
            saveCurrentSelectedLevel()

            drawColor = packs!![current.pack].puzzles[current.level].parts / 2
            savePaletteStatus()

            onLayoutLoad()
        }
    }

    override fun onBackPressed() {
        if (activity_main.isDrawerOpen(GravityCompat.START)) {
            activity_main.closeDrawer(GravityCompat.START)
        } else
            super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.actionbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.refresh_button -> refreshGrid()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.

        if (item.itemId == R.id.send_mail) {
            val intent = Intent(Intent.ACTION_SENDTO)
            intent.data = Uri.parse("mailto:feedback@example.com")
            intent.putExtra(Intent.EXTRA_SUBJECT, "Equalide feedback")
            val appList = this.packageManager.queryIntentActivities(intent,
                PackageManager.MATCH_DEFAULT_ONLY or PackageManager.GET_RESOLVED_FILTER)
            if (appList.size == 1) {
                intent.setClassName(
                    appList[0].activityInfo.packageName,
                    appList[0].activityInfo.name
                )
                startActivity(intent)
            } else
                startActivity(Intent.createChooser(intent, "Send feedback"))
        } else {
            for (i in 0 until packIds.size)
                if (item.itemId == packIds[i]) {
                    selectedPack = i
                    menu?.getItem(selectedPack)?.isChecked = true
                    break
                }

            if (packs!![selectedPack].opened) {
                var levelData = ""
                for (level in packs!![selectedPack].puzzles)
                    levelData += if (level.solved) "s" else if (level.opened) "o" else "c"

                val intent = Intent(this, SelectLevel::class.java).apply {
                    putExtra("pack", (selectedPack + 1).toString())
                    putExtra("level data", levelData)
                }
                onSelectScreen = true
                startActivity(intent)
                overridePendingTransition(R.anim.left_right_enter, R.anim.left_right_exit)
            }
        }
        return true
    }

    fun onLayoutLoad() {
        calculateResolution()
        supportActionBar?.title = "Equalide   ${current.pack + 1}-${(current.level + 1).toString().padStart(2, '0')}"

        loadedPuzzle = packs!![current.pack].puzzles[current.level]

        colors = resources.getIntArray(resources.getIdentifier(
                "colors_for_${loadedPuzzle!!.parts}_parts",
                "array", this.packageName))
        createColorPalette(loadedPuzzle!!.parts)
        if (fabIsShowed) {
            hideColorPalette()
            fabIsShowed = false
        }
        loadPuzzle(loadedPuzzle!!)
    }

    private fun loadUserProgress() {
        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        val packProgress = preferences.getString("Pack progress", null)
        val levelProgress = preferences.getString("Level progress", null)
        val levelPartition = preferences.getString("Level partition", null)
        val currentState = preferences.getString("Current position", null)
        fabIsShowed = preferences.getBoolean("Fab status", false)
        skipSolved = preferences.getBoolean("Skip status", true)
        drawColor = preferences.getInt("Palette status", 0)

        if (packProgress == null || levelProgress == null) {
            packs!![0].opened = true
            for (i in 0 until openDelta)
                packs!![0].puzzles[i].opened = true
        } else {
            // Load pack data
            for (i in 0 until packProgress.length) {
                when (packProgress[i]) {
                    's' -> {
                        packs!![i].opened = true
                        packs!![i].solved = true
                        menu!!.findItem(packIds[i]).icon =
                                ContextCompat.getDrawable(this, R.drawable.ic_star)
                    }
                    'o' -> {
                        packs!![i].opened = true
                        menu!!.findItem(packIds[i]).icon =
                                ContextCompat.getDrawable(this, R.drawable.ic_lock_open)
                    }
                }
            }

            // Load level data
            val levelData = levelProgress.split("\n")
            for (i in 0 until levelData.size - 1)
                for (j in 0 until levelData[i].length) {
                when (levelData[i][j]) {
                    's' -> {
                        packs!![i].puzzles[j].opened = true
                        packs!![i].puzzles[j].solved = true
                    }
                    'o' -> packs!![i].puzzles[j].opened = true
                }
            }
        }

        // Load current level
        if (currentState != null) {
            current.pack = currentState.substring(0..0).toInt()
            current.level = currentState.substring(1).toInt()
        }

        // Load current level partition
        if (levelPartition != null) {
            val puzzle = packs!![current.pack].puzzles[current.level]
            if (levelPartition.length == puzzle.width * puzzle.height)
                puzzle.setPartition(levelPartition)
            else {
                puzzle.refresh()
                Log.d("ERORR", "Incorrect load ocurred!")
            }
        }

        // Show fab if exited on opened fab
        if (fabIsShowed) {
            current.levelSolved = true

            if (!checkIfAllLevelsSolved() || current.level != packSize - 1 || current.pack != packIds.size - 1)
                findViewById<FloatingActionButton>(R.id.fab).show()
        }
    }

    private fun saveUserProgress() {
        var packProgress = ""
        for (pack in packs!!)
            packProgress += if (pack.solved) "s" else if (pack.opened) "o" else "c"

        var levelProgress = ""
        for (pack in packs!!) {
            for (level in pack.puzzles)
                levelProgress += if (level.solved) "s" else if (level.opened) "o" else "c"
            levelProgress += "\n"
        }

        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        with(preferences.edit()) {
            putString("Pack progress", packProgress)
            putString("Level progress", levelProgress)
            apply()
        }
    }

    private fun savePartition() {
        val levelPartition = packs!![current.pack].puzzles[current.level].getPartition()

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
            putInt("Palette status", drawColor)
            apply()
        }
    }

    private fun saveSkipStatus() {
        val preferences = getSharedPreferences(
            getString(R.string.preference_file_key), Context.MODE_PRIVATE)
        with(preferences.edit()) {
            putBoolean("Skip status", skipSolved)
            apply()
        }
    }

    private fun loadPacks(): Array<Pack> {
        val array = Array(packIds.size, { _ -> ArrayList<Puzzle>(0)})
        var file: InputStream? = null
        var parts = 2

        for (i in 1..packIds.size)
            for (j in 1..packSize) {
                try {
                    file = assets.open("$i/${j.toString().padStart(2, '0')}-$parts.txt")
                } catch (_ : Exception) {
                    try {
                        parts++
                        file = assets.open("$i/${j.toString().padStart(2, '0')}-$parts.txt")
                    }
                    catch (e: Exception) {
                        Log.d("ERROR", e.toString())
                    }
                }
                array[i - 1].add(
                    Puzzle(
                        file!!.bufferedReader().use { it.readText() },
                        parts
                    )
                )
            }
        return Array(packIds.size, { i -> Pack(array[i].toTypedArray()) } )
    }

    private fun calculateResolution() {
        val contentArea = findViewById<LinearLayout>(R.id.content_view)

        gridArea.width = contentArea.width
        colorPaletteSize = gridArea.width / 5
        gridArea.height = (contentArea.height - resources.getDimension(R.dimen.puzzle_grid_margin_top)
            - resources.getDimension(R.dimen.puzzle_grid_margin_bottom) - colorPaletteSize).toInt()

        contentArea.setOnTouchListener(gridListener)
    }

    private fun createColorPalette(numOfColors: Int) {
        val palette = findViewById<LinearLayout>(R.id.color_palette)

        for (i in 0 until numOfColors) {
            val colorButton = ImageButton(this)

            // Set size
            val params = LinearLayout.LayoutParams(this.colorPaletteSize,this.colorPaletteSize)
            colorButton.layoutParams = params

            // Set color
            val drawable = ContextCompat.getDrawable(this,
                R.drawable.primitive_border
            ) as GradientDrawable
            drawable.setColor(colors!![i])
            colorButton.background = drawable

            // Set background image
            if (i == drawColor)
                colorButton.setImageDrawable(ContextCompat.getDrawable(this,
                    R.drawable.ic_edit
                ))

            colorButton.tag = "colorButton_" + i.toString()
            colorButton.setOnClickListener(colorPaletteListener)
            palette.addView(colorButton)
        }
    }

    private fun loadPuzzle(puzzle: Puzzle) {
        val grid = findViewById<GridLayout>(R.id.puzzle_grid)

        grid.columnCount = puzzle.width
        grid.rowCount = puzzle.height
        primitiveSize = minOf(gridArea.width / puzzle.width, gridArea.height / puzzle.height)

        val gridParams = LinearLayout.LayoutParams(puzzle.width * primitiveSize, puzzle.height * primitiveSize)
        gridParams.bottomMargin = (gridArea.height - puzzle.height * primitiveSize) / 2
        grid.layoutParams = gridParams

        for (i in 0 until puzzle.height)
            for (j in 0 until puzzle.width) {
                val primitive = Button(this)

                // Set size
                val params = LinearLayout.LayoutParams(primitiveSize, primitiveSize)
                primitive.layoutParams = params

                // Set color
                val drawable = ContextCompat.getDrawable(
                    this,
                    R.drawable.primitive_border
                ) as GradientDrawable

                drawable.setColor(
                    when (puzzle[i, j]) {
                        'b' -> Color.BLACK
                        'w' -> Color.WHITE
                        else -> colors!![puzzle[i, j].toInt() - 48]
                    }
                )
                primitive.background = drawable

                primitive.tag = intArrayOf(i, j)
                primitive.setOnTouchListener { _: View, _: MotionEvent -> false }
                primitive.isClickable = false
                grid.addView(primitive)
            }
    }

    private fun refreshGrid() {
        loadedPuzzle?.refresh()
        savePartition()

        if (!current.levelSolved) {
            val grid = findViewById<GridLayout>(R.id.puzzle_grid)

            for (i in 0 until grid.childCount) {
                val primitive = grid.getChildAt(i) as Button
                val coords = primitive.tag as IntArray
                val background = primitive.background as GradientDrawable
                background.setColor(if (loadedPuzzle!![coords[0], coords[1]] == 'b') Color.BLACK else Color.WHITE)
                primitive.background = background
            }
        } else {
            toast?.cancel()

            refreshContentArea()
            findViewById<FloatingActionButton>(R.id.fab).hide()
            saveFabStatus(false)

            current.levelSolved = false

            drawColor = packs!![current.pack].puzzles[current.level].parts / 2
            savePaletteStatus()
            onLayoutLoad()
        }
    }

    private fun refreshContentArea() {
        val grid = findViewById<GridLayout>(R.id.puzzle_grid)
        val picker = findViewById<LinearLayout>(R.id.color_palette)

        grid.removeAllViews()
        picker.removeAllViews()
    }

    private fun handleSolvedPuzzle() {
        skipSolved = !packs!![current.pack].puzzles[current.level].solved
        saveSkipStatus()

        packs!![current.pack].puzzles[current.level].solved = true

        hideColorPalette()

        if (!packs!![current.pack].solved && checkIfPackSolved(packs!![current.pack])) {
            packs!![current.pack].solved = true
            menu!!.findItem(packIds[current.pack]).icon =
                    ContextCompat.getDrawable(this, R.drawable.ic_star)

            toast?.setText("Pack ${current.pack + 1} solved!")
            toast?.duration = Toast.LENGTH_LONG
        } else {
            toast?.setText("Puzzle solved!")
            toast?.duration = Toast.LENGTH_SHORT
        }
        toast?.show()

        if (!checkIfAllLevelsSolved() || current.level != packSize - 1 || current.pack != packIds.size - 1) {
            findViewById<FloatingActionButton>(R.id.fab).show()
            saveFabStatus(true)
            openNextLevels()
            saveUserProgress()
        } else {
            activity_main.openDrawer(GravityCompat.START)
            saveFabStatus(true)
        }
    }

    private fun hideColorPalette() {
        val palette = findViewById<LinearLayout>(R.id.color_palette)

        for (i in 0 until palette.childCount)
            palette.getChildAt(i).setBackgroundColor(Color.BLACK)
        current.levelSolved = true
    }

    private fun checkIfPackSolved(pack: Pack): Boolean {
        var solved = true

        for (i in 0 until packSize)
            if (!pack.puzzles[i].solved) {
                solved = false
                break
            }

        return solved
    }

    private fun checkIfAllLevelsSolved(): Boolean {
        var solved = true

        mainLoop@ for (pack in packs!!)
            for (puzzle in pack.puzzles)
                if (!puzzle.solved) {
                    solved = false
                    break@mainLoop
                }
        return solved
    }

    private fun openNextLevels() {
        // Open levels in current pack
        if (current.level <= packSize - 1 - openDelta) {
            for (i in 1..openDelta)
                packs!![current.pack].puzzles[current.level + i].opened = true
        } else {
            // Open next pack if it exists and is locked
            if (current.pack != packIds.size - 1 && !packs!![current.pack + 1].opened) {
                packs!![current.pack + 1].opened = true
                menu!!.findItem(packIds[current.pack + 1]).icon =
                        ContextCompat.getDrawable(this, R.drawable.ic_lock_open)
            }

            if (current.level != packSize - 1) {
                // Open levels until end of pack
                for (i in (current.level + 1)..(packSize - 1))
                    packs!![current.pack].puzzles[i].opened = true
                // Open levels in next pack if possible
                if (current.pack != packIds.size - 1) {
                    for (i in 0 until packSize - 1 - current.level)
                        packs!![current.pack + 1].puzzles[i].opened = true
                }
            } else {
                // Open levels in next pack if possible
                if (current.pack != packIds.size - 1)
                    for (i in 0 until openDelta)
                        packs!![current.pack + 1].puzzles[i].opened = true
            }
        }
    }

    private fun selectNextLevel() {
        if (!skipSolved &&
            (current.level != packSize - 1 || current.pack != packIds.size - 1)
        ) {
            if (current.level < packSize - 1)
                current.level++
            else {
                current.level = 0
                current.pack++
            }
        } else {
            // Try to find next level in end of current pack
            for (i in current.level + 1 until packSize)
                if (packs!![current.pack].puzzles[i].opened && !packs!![current.pack].puzzles[i].solved) {
                    current.level = i
                    return
                }

            // Try to find next level in all levels until last in game
            for (i in current.pack + 1 until packIds.size)
                for (j in 0 until packSize)
                    if (packs!![i].puzzles[j].opened && !packs!![i].puzzles[j].solved) {
                        current.level = j
                        current.pack = i
                        return
                    }

            // Try to find next level in all levels before current
            for (i in 0 until current.pack)
                for (j in 0 until packSize)
                    if (packs!![i].puzzles[j].opened && !packs!![i].puzzles[j].solved) {
                        current.level = j
                        current.pack = i
                        return
                    }
        }
    }

    private fun detectPrimitive(ev: MotionEvent): IntArray {
        val x = ev.rawX
        val y = ev.rawY
        val grid = findViewById<GridLayout>(R.id.puzzle_grid)

        for (i in 0 until grid.childCount) {
            val primitive = grid.getChildAt(i) as Button
            val location = IntArray(2)
            primitive.getLocationOnScreen(location)
            if (x > location[0] && x < location[0] + primitiveSize && y > location[1] && y < location[1] + primitiveSize)
                return primitive.tag as IntArray
        }
        return intArrayOf(-1, -1)
    }

    private fun paintPrimitive(coords: IntArray) {
        val grid = findViewById<GridLayout>(R.id.puzzle_grid)
        val primitive: Button = grid.findViewWithTag(coords)
        val background = primitive.background as GradientDrawable

        loadedPuzzle!![coords[0], coords[1]] = if (writeModeOn) drawColor.toString() else "w"
        background.setColor(if (writeModeOn) colors!![drawColor] else Color.WHITE)
        primitive.background = background

        if (loadedPuzzle!!.checkForSolution())
            handleSolvedPuzzle()
        savePartition()
    }

    private val colorPaletteListener = { v: View ->
        val picker = findViewById<LinearLayout>(R.id.color_palette)

        if (v.tag != "colorButton_" + drawColor.toString()) {
            picker.findViewWithTag<ImageButton>(v.tag).setImageDrawable(ContextCompat.getDrawable(this,
                R.drawable.ic_edit
            ))
            picker.findViewWithTag<ImageButton>("colorButton_" + drawColor.toString()).setImageResource(android.R.color.transparent)
            drawColor = v.tag.toString().takeLast(1).toInt()
            savePaletteStatus()
        }
    }

    private val gridListener = { _: View, e: MotionEvent ->
        val coords = detectPrimitive(e)

        if (!coords.contentEquals(intArrayOf(-1, -1)) && loadedPuzzle!![coords[0], coords[1]] != 'b' && !current.levelSolved) {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    prevTouchCoords = coords.copyOf()
                    writeModeOn = (loadedPuzzle!![coords[0], coords[1]].toInt() - 48) != drawColor
                    paintPrimitive(coords)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (prevTouchCoords == null) {
                        writeModeOn = (loadedPuzzle!![coords[0], coords[1]].toInt() - 48) != drawColor
                        prevTouchCoords = intArrayOf(-1, -1)
                    }
                    if (!coords.contentEquals(prevTouchCoords!!)) {
                        val colorMatch = (loadedPuzzle!![coords[0], coords[1]].toInt() - 48) == drawColor
                        if (colorMatch && !writeModeOn || !colorMatch && writeModeOn) {
                            prevTouchCoords = coords.copyOf()
                            paintPrimitive(coords)
                        }
                    }
                }
                MotionEvent.ACTION_UP -> prevTouchCoords = null
                else -> Log.d("TAG", "ERROR")
            }
        }
        true
    }

    private val fabListener = {v: View ->
        toast?.cancel()

        (v as FloatingActionButton).hide()
        refreshContentArea()
        saveFabStatus(false)

        current.levelSolved = false
        selectNextLevel()
        saveCurrentSelectedLevel()
        packs!![current.pack].puzzles[current.level].refresh()
        savePartition()

        drawColor = packs!![current.pack].puzzles[current.level].parts / 2
        savePaletteStatus()

        onLayoutLoad()
    }
}
