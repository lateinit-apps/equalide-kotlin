package equalide.kotlin

import android.os.Bundle
import android.support.v4.view.GravityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.support.design.widget.NavigationView
import android.support.design.widget.CoordinatorLayout
import android.support.design.widget.FloatingActionButton
import android.util.Log
import android.view.*
import android.view.ViewGroup
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.Gravity
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.widget.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_view.*

typealias Pack = Array<Puzzle>

class MainView : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    // Sizes
    private val gridArea = object {
        var width: Int = 0
        var height: Int = 0
    }
    private var colorPickerSize: Int = 0
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

    // Walkthrough related
    private var solved: Boolean = false
    private var currentLevel: Int = 0
    private var maxLevel: Int = 0

    // Other
    private var menu: Menu? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)

        packs = loadPacks()
        menu = findViewById<NavigationView>(R.id.nav_view).menu

        val grid = findViewById<GridLayout>(R.id.grid)
        grid.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                grid.viewTreeObserver.removeOnGlobalLayoutListener(this)
                onLayoutLoad()
            }
        })
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else
            super.onBackPressed()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.actionbar, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        when (item.itemId) {
            R.id.clear_button -> refreshGrid()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        var selectedLevel = 0

        for (i in 0 until packIds.size)
            if (item.itemId == packIds[i]) {
                selectedLevel = i
                break
            }

        if (selectedLevel <= maxLevel) {
            drawer_layout.closeDrawer(GravityCompat.START)
            currentLevel = selectedLevel
            refreshContentArea()
            solved = false
            onLayoutLoad()
        }
        return true
    }

    fun onLayoutLoad() {
        supportActionBar?.title = "Level " + (currentLevel + 1).toString()

        calculateResolutionValues()

        loadedPuzzle = puzzles!![currentLevel]
        loadedPuzzle!!.refresh()

        drawColor = loadedPuzzle!!.parts / 2
        colors = resources.getIntArray(resources.getIdentifier(
                "primitive_colors_for_" + loadedPuzzle!!.parts.toString(),
                "array", this.packageName))
        addColors(loadedPuzzle!!.parts)
        loadPuzzle(loadedPuzzle!!)
    }

    private fun calculateResolutionValues() {
        val contentArea = findViewById<LinearLayout>(R.id.content_area)

        gridArea.width = contentArea.width
        colorPickerSize = gridArea.width / 5
        gridArea.height = contentArea.height - (5 * colorPickerSize) / 4

        contentArea.setOnTouchListener(gridListener)
    }

    private fun addColors(numOfColors: Int) {
        val picker = findViewById<LinearLayout>(R.id.color_picker)

        for (i in 0 until numOfColors) {
            val colorButton = ImageButton(this)

            // Set size
            val params = LinearLayout.LayoutParams(this.colorPickerSize,this.colorPickerSize)
            colorButton.layoutParams = params

            // Set color
            val drawable = ContextCompat.getDrawable(this, R.drawable.primitive_border) as GradientDrawable
            drawable.setColor(colors!![i])
            colorButton.background = drawable

            // Set background image
            if (i == drawColor)
                colorButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.color_picker))

            colorButton.tag = "colorButton_" + i.toString()
            colorButton.setOnClickListener(colorPickerListener)
            picker.addView(colorButton)
        }
    }

    private fun loadPuzzle(puzzle: Puzzle) {
        val grid = findViewById<GridLayout>(R.id.grid)

        grid.columnCount = puzzle.width
        grid.rowCount = puzzle.height
        primitiveSize = minOf(gridArea.width / puzzle.width, gridArea.height / puzzle.height)

        val gridParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams(
                puzzle.width * primitiveSize, puzzle.height * primitiveSize))
        gridParams.bottomMargin = (gridArea.height - puzzle.height * primitiveSize) / 2
        grid.layoutParams = gridParams

        for (i in 0 until puzzle.height)
            for (j in 0 until puzzle.width){
                val primitive = Button(this)

                // Set size
                val params = LinearLayout.LayoutParams(primitiveSize, primitiveSize)
                primitive.layoutParams = params

                // Set color
                val drawable = ContextCompat.getDrawable(this, R.drawable.primitive_border) as GradientDrawable
                drawable.setColor(if (puzzle.body[i][j] == -2) Color.BLACK else Color.WHITE)
                primitive.background = drawable

                primitive.tag = intArrayOf(i, j)
                primitive.setOnTouchListener { _: View, _: MotionEvent -> false }
                primitive.isClickable = false
                grid.addView(primitive)
            }
    }

    private fun refreshGrid() {
        if (!solved) {
            val grid = findViewById<GridLayout>(R.id.grid)

            for (i in 0 until grid.childCount) {
                val primitive = grid.getChildAt(i) as Button
                val coords = primitive.tag as IntArray
                val background = primitive.background as GradientDrawable
                background.setColor(if (loadedPuzzle!!.body[coords[0]][coords[1]] == -2) Color.BLACK else Color.WHITE)
                primitive.background = background
            }
        }
    }

    private fun handleSolvedPuzzle() {
        val picker = findViewById<LinearLayout>(R.id.color_picker)
        val mainView = findViewById<CoordinatorLayout>(R.id.main_view)

        val drawable = ContextCompat.getDrawable(this, R.drawable.primitive_border) as GradientDrawable
        drawable.setColor(Color.BLACK)

        for (i in 0 until picker.childCount)
            picker.getChildAt(i).background = drawable
        solved = true

        if (currentLevel != 8) {
            unlockNewLevel()
            val fab = FloatingActionButton(this)
            fab.setImageResource(R.drawable.ic_navigate_next)
            fab.size = android.support.design.widget.FloatingActionButton.SIZE_AUTO
            fab.isFocusable = true
            val lay = CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT)
            lay.gravity = Gravity.BOTTOM or Gravity.END
            lay.setMargins(2, 2, 40, 60)
            fab.layoutParams = lay

            val fabId = View.generateViewId()
            fab.id = fabId
            fab.setOnClickListener {
                refreshContentArea()
                val fb = findViewById<FloatingActionButton>(fabId)
                mainView.removeView(fb)
                solved = false
                onLayoutLoad()
            }
            mainView.addView(fab)
            Toast.makeText(this, "Puzzle solved!", Toast.LENGTH_LONG).show()
        } else {
            unlockNewLevel()
            Toast.makeText(this, "You solved all puzzles!", Toast.LENGTH_LONG).show()
        }
    }

    private fun loadPacks() : Array<Pack> {
        val array = Array(packIds.size, { _ -> ArrayList<Puzzle>(0)})

        for (i in 1..packIds.size)
            for (j in 1..packSize) {
                val string =
                    application.assets.open("0" + i.toString() + ".txt").bufferedReader().use {
                        it.readText()
                    }
                array[i - 1].add(Puzzle(string))
            }
        return Array(packIds.size, { i -> array[i].toTypedArray() } )
    }

    private fun refreshContentArea() {
        val grid = findViewById<GridLayout>(R.id.grid)
        val picker = findViewById<LinearLayout>(R.id.color_picker)

        grid.removeAllViews()
        picker.removeAllViews()
    }

    private fun unlockNewLevel() {
        Log.d("TAG", "Solved: " + currentLevel.toString())
        val currentLevelId = packIds[currentLevel]
        val currentMenuItem = menu!!.findItem(currentLevelId)
        currentMenuItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_star)

        if (currentLevel != 8) {
            if (currentLevel == maxLevel) {
                currentLevel++
                maxLevel++
                val nextLevelId = packIds[currentLevel]
                val nextMenuItem = menu!!.findItem(nextLevelId)
                nextMenuItem.icon = ContextCompat.getDrawable(this, R.drawable.ic_lock_open)
            } else
                currentLevel++
        }
    }

    private fun detectPrimitiveBy(ev: MotionEvent) : IntArray {
        val x = ev.rawX
        val y = ev.rawY
        val grid = findViewById<GridLayout>(R.id.grid)

        for (i in 0 until grid.childCount) {
            val primitive = grid.getChildAt(i) as Button
            val location = IntArray(2)
            primitive.getLocationOnScreen(location)
            if (x > location[0] && x < location[0] + primitiveSize && y > location[1] && y < location[1] + primitiveSize)
                return primitive.tag as IntArray
        }
        return intArrayOf(-1, -1)
    }

    private fun drawPrimitive(coords: IntArray) {
        val grid = findViewById<GridLayout>(R.id.grid)
        val primitive: Button = grid.findViewWithTag(coords)
        val background = primitive.background as GradientDrawable

        loadedPuzzle!!.body[coords[0]][coords[1]] = if (writeModeOn) drawColor else -1
        background.setColor(if (writeModeOn) colors!![drawColor] else Color.WHITE)
        primitive.background = background

        if (loadedPuzzle!!.checkForSolution())
            handleSolvedPuzzle()
    }

    private val colorPickerListener = { v: View ->
        val picker = findViewById<LinearLayout>(R.id.color_picker)

        if (v.tag != "colorButton_" + drawColor.toString()) {
            picker.findViewWithTag<ImageButton>(v.tag).setImageDrawable(ContextCompat.getDrawable(this, R.drawable.color_picker))
            picker.findViewWithTag<ImageButton>("colorButton_" + drawColor.toString()).setImageResource(android.R.color.transparent)
            drawColor = v.tag.toString().takeLast(1).toInt()
        }
    }

    private val gridListener = { _: View, e: MotionEvent ->
        val coords = detectPrimitiveBy(e)

        if (!coords.contentEquals(intArrayOf(-1, -1)) && loadedPuzzle!!.body[coords[0]][coords[1]] != -2 && !solved) {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    prevTouchCoords = coords.copyOf()
                    writeModeOn = loadedPuzzle!!.body[coords[0]][coords[1]] != drawColor
                    drawPrimitive(coords)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (prevTouchCoords == null) {
                        writeModeOn = loadedPuzzle!!.body[coords[0]][coords[1]] != drawColor
                        prevTouchCoords = intArrayOf(-1, -1)
                    }
                    if (!coords.contentEquals(prevTouchCoords!!)) {
                        val colorMatch = loadedPuzzle!!.body[coords[0]][coords[1]] == drawColor
                        if (colorMatch && !writeModeOn || !colorMatch && writeModeOn) {
                            prevTouchCoords = coords.copyOf()
                            drawPrimitive(coords)
                        }
                    }

                }
                MotionEvent.ACTION_UP -> prevTouchCoords = null
                else -> Log.d("TAG", "ERROR")
            }
        }
        true
    }
}
