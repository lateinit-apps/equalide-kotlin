package equalide.kotlin.activities

import android.os.Bundle
import android.content.Intent

import android.support.v4.content.ContextCompat
import android.support.v7.app.AppCompatActivity

import android.graphics.Color

import android.view.View
import android.view.Gravity
import android.view.MenuItem
import android.view.ViewTreeObserver.OnGlobalLayoutListener

import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout

import equalide.kotlin.R

class SelectLevel : AppCompatActivity() {

    // Tile related
    private val rowCount = 6
    private val columnCount = 4
    private var tileSize: Int = 0
    private var levelData: String = ""
    private var grid: GridLayout? = null

    // Margin related
    private var horizontalMargin: Int = 0
    private var verticalMargin: Int = 0
    private var primitiveMargin: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_select_level)

        grid = findViewById(R.id.level_grid)

        val packNumber = intent.getStringExtra("pack")
        supportActionBar?.title = "Pack $packNumber"

        levelData = intent.getStringExtra("level data")

        // Listener to detect when layout is loaded to get it's resolution properties
        grid?.viewTreeObserver?.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                grid?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                calculateViewsSizes()
                createGrid(levelData)
            }
        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.right_left_enter, R.anim.right_left_exit)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            overridePendingTransition(R.anim.right_left_enter, R.anim.right_left_exit)
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun calculateViewsSizes() {
        val selectView = findViewById<LinearLayout>(R.id.activity_select_level)

        tileSize = (0.195 * selectView.width).toInt()
        primitiveMargin = ((0.2 / 14) * selectView.width).toInt()

        horizontalMargin = (4 * (0.2 / 14) * selectView.width).toInt()
        verticalMargin = (selectView.height - 6 * tileSize - 12 * primitiveMargin) / 2
    }

    private fun createGrid(levelData: String) {
        grid?.rowCount = rowCount
        grid?.columnCount = columnCount

        val gridParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
        gridParams.setMargins(horizontalMargin, verticalMargin, horizontalMargin, verticalMargin)
        grid?.layoutParams = gridParams

        val colorPrimary = ContextCompat.getColor(this, R.color.colorPrimary)
        val colorPrimaryDark = ContextCompat.getColor(this, R.color.colorPrimaryDark)

        for (i in 0 until rowCount)
            for (j in 0 until columnCount){
                val tile = Button(this)
                val level = i * columnCount + j

                // Set size and margins
                val params = GridLayout.LayoutParams()
                params.width = tileSize
                params.height = tileSize
                params.setMargins(primitiveMargin, primitiveMargin, primitiveMargin, primitiveMargin)
                tile.layoutParams = params

                if (levelData[level] != 'c') {
                    tile.gravity = Gravity.CENTER

                    tile.text = (level + 1).toString()
                    tile.textSize = 17.toFloat()

                    tile.setTextColor(if (levelData[level] == 's')
                        colorPrimary else colorPrimaryDark)
                    tile.setBackgroundColor(if (levelData[level] == 's')
                        colorPrimaryDark else Color.WHITE)
                } else
                    tile.setBackgroundColor(Color.WHITE)

                tile.tag = level

                tile.setOnClickListener(levelButtonListener)

                grid?.addView(tile)
            }
    }

    private val levelButtonListener = { v: View ->
        if (levelData[v.tag as Int] != 'c') {
            v.setBackgroundColor(if (levelData[v.tag as Int] == 's')
                ContextCompat.getColor(this, R.color.grey_800) else
                ContextCompat.getColor(this, R.color.grey_400))

            val intent = Intent(this, Main::class.java).apply {
                putExtra("selected level", v.tag as Int)
            }
            startActivity(intent)
            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }
}
