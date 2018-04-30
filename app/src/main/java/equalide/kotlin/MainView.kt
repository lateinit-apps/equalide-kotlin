package equalide.kotlin

import android.graphics.Color
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.view.GravityCompat
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.widget.Button
import android.widget.GridLayout
import android.util.Log
import android.view.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.main_view.*
import android.widget.LinearLayout
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.RelativeLayout
import android.support.v4.content.ContextCompat
import android.graphics.drawable.GradientDrawable

class MainView : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {
    var height: Int = 0
    var width:  Int = 0
    var colorPickerSize: Int = 0
    var dp: Float = 0f
    var drawColor: Int = 0
    var colors: IntArray? = null

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
        Log.d("TAG2","$height + $width + $dp + $colorPickerSize")

        val pzl = Puzzle("000200\n" +
                "202223\n" +
                "222333\n" +
                "233313\n" +
                "013111\n" +
                "011100\n" +
                "010000")
        drawColor = pzl.parts / 2
        colors = resources.getIntArray(resources.getIdentifier(
                "primitive_colors_for_" + pzl.parts.toString(),
                "array", this.packageName))
        addColors(pzl.parts)
        loadPuzzle(pzl)
        //Log.d("TAG2","${pzl.width} + ${pzl.height} + ${pzl.parts} + ${pzl.body}")
    }

    private fun calculateResolutionValues() {
        val contentArea = findViewById<RelativeLayout>(R.id.content_area)
        width = contentArea.width
        dp = resources.displayMetrics.density
        colorPickerSize = width / 7
        height = contentArea.height - colorPickerSize

        val grid = findViewById<GridLayout>(R.id.grid)
        val params = RelativeLayout.LayoutParams(ViewGroup.LayoutParams(width, height))
        params.addRule(RelativeLayout.CENTER_HORIZONTAL)
        params.addRule(RelativeLayout.CENTER_VERTICAL)
        grid.layoutParams = params
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

            // Add to picker
            picker.addView(colorButton)
        }
    }

    private fun loadPuzzle(puzzle: Puzzle) {
        val grid = findViewById<GridLayout>(R.id.grid)

        grid.columnCount = puzzle.width
        grid.rowCount = puzzle.height
        val size = minOf(width / puzzle.width, height / puzzle.height)

        for (i in 0 until puzzle.height)
            for (j in 0 until puzzle.width){
                val primitive = Button(this)

                // Set size
                val params = LinearLayout.LayoutParams(size, size)
                primitive.layoutParams = params

                // Set color
                val drawable = ContextCompat.getDrawable(this, R.drawable.primitive_border) as GradientDrawable
                drawable.setColor(if (puzzle.body[i][j] == 0) Color.BLACK else Color.WHITE)
                primitive.background = drawable

                grid.addView(primitive)
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
            //R.id.action_settings -> return true
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
