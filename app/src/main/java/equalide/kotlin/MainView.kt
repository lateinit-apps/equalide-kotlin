package equalide.kotlin

import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.util.Log
import android.view.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_view.*
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.support.v4.content.ContextCompat
import android.graphics.drawable.GradientDrawable
import android.widget.*

class MainView : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var height: Int = 0
    private var width:  Int = 0
    private var colorPickerSize: Int = 0
    private var dp: Float = 0f
    private var drawColor: Int = 0
    private var prevTouchCoords: IntArray? = null
    private var writeModeOn: Boolean = false
    private var primitiveSize: Int = 0

    // -2 out of puzzle
    // -1 white
    private var colors: IntArray? = null
    private var puzzle: Puzzle? = null

    private val colorPickerListener = { v: View ->
        val picker = findViewById<LinearLayout>(R.id.color_picker)

        if (v.tag != "colorButton_" + drawColor.toString()) {
            picker.findViewWithTag<Button>(v.tag).text = "X"
            picker.findViewWithTag<Button>("colorButton_" + drawColor.toString()).text = ""
            drawColor = v.tag.toString().takeLast(1).toInt()
        }
    }

    private val passToGrid = { _: View, _: MotionEvent -> false }

    private val gridListener = { _: View, e: MotionEvent ->
        val coords = detectPrimitiveBy(e)

        if (!coords.contentEquals(intArrayOf(-1, -1)) && puzzle!!.body[coords[0]][coords[1]] != -2) {
            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    Log.d("TAG", "DOWN")

                    prevTouchCoords = coords.copyOf()
                    writeModeOn = puzzle!!.body[coords[0]][coords[1]] != drawColor

                    val grid = findViewById<GridLayout>(R.id.grid)
                    val primitive: Button = grid.findViewWithTag(coords)
                    val background = primitive.background as GradientDrawable

                    puzzle!!.body[coords[0]][coords[1]] = if (writeModeOn) drawColor else -1
                    background.setColor(if (writeModeOn) colors!![drawColor] else Color.WHITE)
                    primitive.background = background
                }
                MotionEvent.ACTION_MOVE -> {
                    if (prevTouchCoords == null) {
                        writeModeOn = puzzle!!.body[coords[0]][coords[1]] != drawColor
                        prevTouchCoords = intArrayOf(-1, -1)
                    }
                    if (!coords.contentEquals(prevTouchCoords!!)) {
                        val colorMatch = puzzle!!.body[coords[0]][coords[1]] == drawColor
                        if (colorMatch && !writeModeOn || !colorMatch && writeModeOn) {
                            prevTouchCoords = coords.copyOf()

                            val grid = findViewById<GridLayout>(R.id.grid)
                            val primitive: Button = grid.findViewWithTag(coords)
                            val background = primitive.background as GradientDrawable

                            puzzle!!.body[coords[0]][coords[1]] = if (writeModeOn) drawColor else -1
                            background.setColor(if (writeModeOn) colors!![drawColor] else Color.WHITE)
                            primitive.background = background
                            primitive.invalidate()
                        }
                    }
                }
                MotionEvent.ACTION_UP -> {
                    Log.d("TAG", "UP")
                    prevTouchCoords = null
                }
                else -> {
                    Log.d("TAG", "SHIT")
                }
            }
        }
        true
    }

    fun detectPrimitiveBy(ev: MotionEvent) : IntArray {
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this, drawer_layout, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()
        nav_view.setNavigationItemSelectedListener(this)

        val gridArea = findViewById<GridLayout>(R.id.grid)
        gridArea.viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                gridArea.viewTreeObserver.removeOnGlobalLayoutListener(this)
                onLayoutLoad()
            }
        })
    }

    fun onLayoutLoad() {
        calculateResolutionValues()
        //Log.d("TAG2","$height + $width + $dp + $colorPickerSize")

        puzzle = Puzzle("000200\n" +
                "202223\n" +
                "222333\n" +
                "233313\n" +
                "013111\n" +
                "011100\n" +
                "010000")
     /*   puzzle = Puzzle("0200\n" +
                "2223\n" +
                "2333\n" +
                "3313\n" +
                "3111")*/
        drawColor = puzzle!!.parts / 2
        colors = resources.getIntArray(resources.getIdentifier(
                "primitive_colors_for_" + puzzle!!.parts.toString(),
                "array", this.packageName))
        addColors(puzzle!!.parts)
        loadPuzzle(puzzle!!)

        //Log.d("TAG2","${pzl.width} + ${pzl.height} + ${pzl.parts} + ${pzl.body}")
    }

    private fun calculateResolutionValues() {
        val contentArea = findViewById<LinearLayout>(R.id.content_area)
        width = contentArea.width
        dp = resources.displayMetrics.density
        colorPickerSize = width / 7
        height = contentArea.height - colorPickerSize

        contentArea.setOnTouchListener(gridListener)
    }

    private fun addColors(numOfColors: Int) {
        val picker = findViewById<LinearLayout>(R.id.color_picker)

        for (i in 0 until numOfColors) {
            val colorButton = Button(this)

            // Set size
            val params = LinearLayout.LayoutParams(this.colorPickerSize,this.colorPickerSize)
            colorButton.layoutParams = params

            // Set color
            val drawable = ContextCompat.getDrawable(this, R.drawable.primitive_border) as GradientDrawable
            drawable.setColor(colors!![i])
            colorButton.background = drawable

            // Set current selected color
            if (i == drawColor)
                colorButton.text = "X"

            // Set tag
            colorButton.tag = "colorButton_" + i.toString()

            // Set event handler
            colorButton.setOnClickListener(colorPickerListener)

            // Add to picker
            picker.addView(colorButton)
        }
    }

    private fun loadPuzzle(puzzle: Puzzle) {
        val grid = findViewById<GridLayout>(R.id.grid)

        grid.columnCount = puzzle.width
        grid.rowCount = puzzle.height
        primitiveSize = minOf(width / puzzle.width, height / puzzle.height)

        val gridParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams(
                puzzle.width * primitiveSize, puzzle.height * primitiveSize))
        gridParams.bottomMargin = (height - puzzle.height * primitiveSize) / 2

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

                // Set tag
                primitive.tag = intArrayOf(i, j)

                // Set event handlers
                primitive.setOnTouchListener(passToGrid)
                primitive.isClickable = false

                // Add to grid
                grid.addView(primitive)
            }
    }

    private fun refreshGrid() {
        val grid = findViewById<GridLayout>(R.id.grid)

        for (i in 0 until grid.childCount) {
            val primitive = grid.getChildAt(i) as Button
            val coords = primitive.tag as IntArray
            val background = primitive.background as GradientDrawable
            background.setColor(if (puzzle!!.body[coords[0]][coords[1]] == -2) Color.BLACK else Color.WHITE)
            primitive.background = background
        }
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
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
            R.id.clear_button -> {
                refreshGrid()
                return true
            }
            else -> return super.onOptionsItemSelected(item)
        }
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        // Handle navigation view item clicks here.
        when (item.itemId) {
            R.id.nav_camera -> {
                // Handle the camera action
            }
            R.id.nav_gallery -> {

            }
            R.id.nav_slideshow -> {

            }
            R.id.nav_manage -> {

            }
            R.id.nav_share -> {

            }
            R.id.nav_send -> {

            }
        }

        drawer_layout.closeDrawer(GravityCompat.START)
        return true
    }
}
