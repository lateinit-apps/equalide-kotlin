package equalide.kotlin.activities

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
import equalide.kotlin.logic.Pack
import equalide.kotlin.logic.Puzzle
import equalide.kotlin.R
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_view.*
import java.io.InputStream

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

    // Walkthrough related
    private val current = object {
        var level: Int = 0
        var pack: Int = 0
        var levelSolved: Boolean = false
    }
    private val openDelta: Int = 3

    // Other
    private var menu: Menu? = null
    private var fab: FloatingActionButton? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
            this, drawer_layout, toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close
        )
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)

        packs = loadPacks()
        packs!![0].opened = true
        for (i in 0 until openDelta)
            packs!![0].puzzles[i].opened = true

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
        var selectedPack = 0

        for (i in 0 until packIds.size)
            if (item.itemId == packIds[i]) {
                selectedPack = i
                break
            }

        if (packs!![selectedPack].opened) {
            if (fab != null) {
                findViewById<CoordinatorLayout>(R.id.main_view).removeView(fab)
                fab = null
            }
        }
        return true
    }

    fun onLayoutLoad() {
        calculateResolution()

        Log.d("ERROR", "ASDASD")
        loadedPuzzle = packs!![current.pack].puzzles[current.level]
        loadedPuzzle!!.refresh()

        drawColor = loadedPuzzle!!.parts / 2
        colors = resources.getIntArray(resources.getIdentifier(
                "primitive_colors_for_" + loadedPuzzle!!.parts.toString(),
                "array", this.packageName))
        createColorPalette(loadedPuzzle!!.parts)
        loadPuzzle(loadedPuzzle!!)
    }

    private fun loadPacks() : Array<Pack> {
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
        val contentArea = findViewById<LinearLayout>(R.id.content_area)

        gridArea.width = contentArea.width
        colorPaletteSize = gridArea.width / 5
        gridArea.height = contentArea.height - (5 * colorPaletteSize) / 4

        contentArea.setOnTouchListener(gridListener)
    }

    private fun createColorPalette(numOfColors: Int) {
        val picker = findViewById<LinearLayout>(R.id.color_picker)

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
                    R.drawable.color_picker
                ))

            colorButton.tag = "colorButton_" + i.toString()
            colorButton.setOnClickListener(colorPaletteListener)
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
                val drawable = ContextCompat.getDrawable(this,
                    R.drawable.primitive_border
                ) as GradientDrawable
                drawable.setColor(if (puzzle[i, j] == 'b') Color.BLACK else Color.WHITE)
                primitive.background = drawable

                primitive.tag = intArrayOf(i, j)
                primitive.setOnTouchListener { _: View, _: MotionEvent -> false }
                primitive.isClickable = false
                grid.addView(primitive)
            }
    }

    private fun refreshGrid() {
        if (!current.levelSolved) {
            val grid = findViewById<GridLayout>(R.id.grid)

            for (i in 0 until grid.childCount) {
                val primitive = grid.getChildAt(i) as Button
                val coords = primitive.tag as IntArray
                val background = primitive.background as GradientDrawable
                background.setColor(if (loadedPuzzle!![coords[0], coords[1]] == 'b') Color.BLACK else Color.WHITE)
                primitive.background = background
            }

            loadedPuzzle?.refresh()
        }
    }

    private fun refreshContentArea() {
        val grid = findViewById<GridLayout>(R.id.grid)
        val picker = findViewById<LinearLayout>(R.id.color_picker)

        grid.removeAllViews()
        picker.removeAllViews()
    }

    private fun handleSolvedPuzzle() {
        packs!![current.pack].puzzles[current.level].solved = true

        val picker = findViewById<LinearLayout>(R.id.color_picker)

        val drawable = ContextCompat.getDrawable(this, R.drawable.primitive_border) as GradientDrawable
        drawable.setColor(Color.BLACK)

        for (i in 0 until picker.childCount)
            picker.getChildAt(i).background = drawable
        current.levelSolved = true

        if (!packs!![current.pack].solved && checkIfPackSolved(packs!![current.pack])) {
            packs!![current.pack].solved = true
            menu!!.findItem(packIds[current.pack]).icon =
                    ContextCompat.getDrawable(this, R.drawable.ic_star)
            Toast.makeText(this, "Pack ${current.pack + 1} solved!", Toast.LENGTH_LONG).show()
        } else
            Toast.makeText(this, "Puzzle solved!", Toast.LENGTH_LONG).show()

        if (current.level != packSize - 1 || current.pack != packIds.size - 1) {
            unlockNextLevel()
            createFabButton()
        }
    }

    private fun createFabButton() {
        val mainView = findViewById<CoordinatorLayout>(R.id.main_view)

        fab = FloatingActionButton(this)
        fab?.id = View.generateViewId()
        fab?.setImageResource(R.drawable.ic_navigate_next)
        fab?.size = android.support.design.widget.FloatingActionButton.SIZE_AUTO
        fab?.isFocusable = true

        val layoutParams = CoordinatorLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
            ViewGroup.LayoutParams.WRAP_CONTENT)
        layoutParams.gravity = Gravity.BOTTOM or Gravity.END
        layoutParams.setMargins(2, 2, 40, 60)
        fab?.layoutParams = layoutParams

        fab?.setOnClickListener {
            refreshContentArea()
            mainView.removeView(fab)
            current.levelSolved = false
            onLayoutLoad()
        }
        mainView.addView(fab)
    }

    private fun checkIfPackSolved(pack: Pack) : Boolean {
        var solved = true

        for (i in 0 until packSize)
            if (!pack.puzzles[i].solved) {
                solved = false
                break
            }

        return solved
    }

    private fun unlockNextLevel() {
        // Open levels in current pack
        if (current.level < packSize - 1 - openDelta) {
            for (i in 1..openDelta)
                packs!![current.pack].puzzles[current.level + i].opened = true
            current.level++
        } else {
            // Open next pack if it exists and is locked
            if (current.pack != packIds.size && !packs!![current.pack + 1].opened) {
                packs!![current.pack + 1].opened = true
                menu!!.findItem(packIds[current.pack + 1]).icon =
                        ContextCompat.getDrawable(this, R.drawable.ic_lock_open)
            }

            if (current.level != packSize - 1) {
                // Open levels until end of pack
                for (i in (current.level + 1)..(packSize - 1))
                    packs!![current.pack].puzzles[i].opened = true
                // Open levels in next pack if possible
                if (current.pack != packIds.size) {
                    for (i in 0 until packSize - 1 - current.level)
                        packs!![current.pack + 1].puzzles[i].opened = true
                }
                current.level++
            } else {
                // Open levels in next pack
                current.level = 0
                current.pack++
                for (i in 0 until openDelta)
                    packs!![current.pack].puzzles[i].opened = true
            }
        }
    }

    private fun detectPrimitive(ev: MotionEvent) : IntArray {
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

    private fun paintPrimitive(coords: IntArray) {
        val grid = findViewById<GridLayout>(R.id.grid)
        val primitive: Button = grid.findViewWithTag(coords)
        val background = primitive.background as GradientDrawable

        loadedPuzzle!![coords[0], coords[1]] = if (writeModeOn) drawColor.toString() else "w"
        background.setColor(if (writeModeOn) colors!![drawColor] else Color.WHITE)
        primitive.background = background

        if (loadedPuzzle!!.checkForSolution())
            handleSolvedPuzzle()
    }

    private val colorPaletteListener = { v: View ->
        val picker = findViewById<LinearLayout>(R.id.color_picker)

        if (v.tag != "colorButton_" + drawColor.toString()) {
            picker.findViewWithTag<ImageButton>(v.tag).setImageDrawable(ContextCompat.getDrawable(this,
                R.drawable.color_picker
            ))
            picker.findViewWithTag<ImageButton>("colorButton_" + drawColor.toString()).setImageResource(android.R.color.transparent)
            drawColor = v.tag.toString().takeLast(1).toInt()
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
}
