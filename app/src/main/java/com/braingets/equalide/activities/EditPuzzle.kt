package com.braingets.equalide.activities

import android.Manifest
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.Log

import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity

import android.view.View
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.ViewTreeObserver.OnGlobalLayoutListener

import android.widget.*

import kotlinx.android.synthetic.main.main_screen.*

import com.braingets.equalide.R
import com.braingets.equalide.logic.*

class EditPuzzle : AppCompatActivity() {

    // Different element sizes
    object ContentView {
        var width: Int = 0
        var height: Int = 0
    }
    private var colorPaletteSize: Int = 0
    private var primitiveSize: Int = 0

    // Puzzle related
    private var puzzle: Puzzle? = null

    // Paint related
    private var paintColor: Int = 2
    private var colors: IntArray? = null
    private var eraseMode: Boolean = false
    private var previousPaintCoords: IntArray = IntArray(2)

    // Views related
    private var grid: GridLayout? = null
    private var palette: LinearLayout? = null
    private var editEdgesMode: Int = INCREASE
    private var centerButton: Button? = null
    private var upButton: Button? = null
    private var downButton: Button? = null
    private var leftButton: Button? = null
    private var rightButton: Button? = null

    // Activity related
    private var exportIntent: Intent? = null
    private var createPuzzle: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_puzzle)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportActionBar?.title = "Puzzle editor"

        createPuzzle = intent.getBooleanExtra("create puzzle", false)

        if (!createPuzzle) {
            val puzzleData = intent.getStringExtra("puzzle data")

            if (puzzleData != null)
                puzzle = Puzzle(puzzleData)
        }

        // Find or create views
        grid = findViewById(R.id.puzzle_grid)
        palette = findViewById(R.id.color_palette)
        centerButton = findViewById(R.id.center_button)
        upButton = findViewById(R.id.up_button)
        downButton = findViewById(R.id.down_button)
        leftButton = findViewById(R.id.left_button)
        rightButton = findViewById(R.id.right_button)

        // Add listeners
        grid?.setOnTouchListener(gridListener)
        centerButton?.setOnClickListener(editEdgesButtonsListener)
        upButton?.setOnClickListener(editEdgesButtonsListener)
        downButton?.setOnClickListener(editEdgesButtonsListener)
        leftButton?.setOnClickListener(editEdgesButtonsListener)
        rightButton?.setOnClickListener(editEdgesButtonsListener)

        // Listener to detect when layout is loaded to get it's resolution properties
        grid?.viewTreeObserver?.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                grid?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                onLayoutLoad()
            }
        })
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val exportedFileName = intent?.getStringExtra("exported file name")

        if (exportedFileName != null) {
            Toast.makeText(this, "Exported as $exportedFileName ", Toast.LENGTH_SHORT).show()
            return
        }

        val askForWritePermission = intent?.getBooleanExtra("no write permission", false)

        if (askForWritePermission == true) {
            exportIntent = intent.setClass(this, Exporter::class.java)

            ActivityCompat.requestPermissions(this,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), WRITE_PERMISSION_REQUEST)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        if (requestCode == WRITE_PERMISSION_REQUEST &&
            grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            startService(exportIntent)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_puzzle_actionbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                overridePendingTransition(R.anim.right_left_enter, R.anim.right_left_exit)
            }

            R.id.export_puzzle_button -> if (puzzle != null) {
                val localClassName = localClassName.split(".")
                val className = localClassName[localClassName.lastIndex]

                val intent = Intent(this, Exporter::class.java)
                    .putExtra("text", puzzle?.source)
                    .putExtra("file name", "puzzle.eqld")
                    .putExtra("class name", className)
                startService(intent)
            }

            R.id.save_puzzle_button -> if (puzzle != null) {
                if (puzzle!!.source.any { c -> c != 'b' } && puzzle!!.checkIfValid()) {

                    val puzzleSource = normalizePuzzleSource(
                        puzzle!!.partition.chunked(puzzle!!.width).joinToString("\n"))
                        .replace("b", "0")

                    val intent = Intent(this, Main::class.java)
                        .putExtra("puzzle source", puzzleSource)
                        .putExtra("select level screen", createPuzzle)

                    startActivity(intent)

                    overridePendingTransition(R.anim.right_left_exit, R.anim.right_left_enter)
                } else
                    Toast.makeText(this, "Invalid puzzle!", Toast.LENGTH_SHORT).show()
            }

            R.id.refresh_button -> if (puzzle != null) {
                grid?.removeAllViews()
                puzzle?.refresh()
                renderPuzzle(puzzle!!)
            }

            R.id.edit_edges_button ->
                toggleEditEdgesButtonsVisibility(centerButton?.visibility == View.INVISIBLE)
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onLayoutLoad() {
        calculateViewsSizes()

        colors = resources.getIntArray(resources.getIdentifier(
            "colors_for_puzzle_editor",
            "array", this.packageName))

        addColorPalette(colors!!)

        if (puzzle != null) {
            toggleEditEdgesButtonsVisibility(false)
            renderPuzzle(puzzle!!)
        } else
            toggleEditEdgesButtonsVisibility(true)
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

    private fun toggleEditEdgesButtonsVisibility(visible: Boolean) {
        val visibility = if (visible) View.VISIBLE else View.INVISIBLE

        centerButton?.visibility = visibility
        upButton?.visibility = visibility
        downButton?.visibility = visibility
        leftButton?.visibility = visibility
        rightButton?.visibility = visibility
    }

    private fun toggleEditEdgesCornerButtonsIcons(mode: Int) {
        val upArrow = ContextCompat.getDrawable(this, R.drawable.up_arrow)
        val downArrow = ContextCompat.getDrawable(this, R.drawable.down_arrow)
        val leftArrow = ContextCompat.getDrawable(this, R.drawable.left_arrow)
        val rightArrow = ContextCompat.getDrawable(this, R.drawable.right_arrow)

        when (mode) {
            INCREASE -> {
                upButton?.background = upArrow
                downButton?.background = downArrow
                leftButton?.background = leftArrow
                rightButton?.background = rightArrow
            }

            DECREASE -> {
                upButton?.background = downArrow
                downButton?.background = upArrow
                leftButton?.background = rightArrow
                rightButton?.background = leftArrow
            }
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

        puzzle!![coords[0], coords[1]] = if (eraseMode) "e" else if (paintColor == 0) "b" else paintColor.toString()
    }

    private val gridListener = { _: View, e: MotionEvent ->
        val coords = getGridCoordsFromTouch(e)

        if (!coords.contentEquals(intArrayOf(-1, -1))) {

            when (e.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    previousPaintCoords = coords.copyOf()

                    eraseMode = if (paintColor == 0) puzzle!![coords[0], coords[1]] == 'b'
                        else (puzzle!![coords[0], coords[1]].toInt() - 48) == paintColor

                    paintPrimitive(coords)
                }
                MotionEvent.ACTION_MOVE -> {
                    if (!coords.contentEquals(previousPaintCoords)) {
                        previousPaintCoords = coords.copyOf()

                        if (!eraseMode
                            || (paintColor != 0 && puzzle!![coords[0], coords[1]].toInt() - 48 == paintColor)
                            || (paintColor == 0 && puzzle!![coords[0], coords[1]] == 'b'))
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
        }
    }

    private val editEdgesButtonsListener = { v: View ->
        when (v.id) {
            R.id.center_button -> {
                editEdgesMode = if (editEdgesMode == INCREASE) DECREASE else INCREASE
                toggleEditEdgesCornerButtonsIcons(editEdgesMode)
            }
            R.id.up_button -> puzzle = puzzle?.getChangedBySide(Direction.UP, editEdgesMode)
            R.id.down_button -> puzzle = puzzle?.getChangedBySide(Direction.DOWN, editEdgesMode)
            R.id.left_button -> puzzle = puzzle?.getChangedBySide(Direction.LEFT, editEdgesMode)
            R.id.right_button -> puzzle = puzzle?.getChangedBySide(Direction.RIGHT, editEdgesMode)
        }

        if (v.id != R.id.center_button) {
            if (editEdgesMode == INCREASE && puzzle == null)
                puzzle = Puzzle("1")

            grid?.removeAllViews()

            if (puzzle != null)
                renderPuzzle(puzzle!!)
        }
    }
}
