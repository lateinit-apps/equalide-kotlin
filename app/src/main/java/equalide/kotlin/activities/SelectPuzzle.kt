package equalide.kotlin.activities

import android.content.Intent
import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewTreeObserver
import android.widget.Button
import android.widget.GridLayout
import android.widget.LinearLayout
import equalide.kotlin.R

class SelectPuzzle : AppCompatActivity() {

    private var primitiveSize: Int = 0
    var horizontalMargin: Int = 0
    var verticalMargin: Int = 0
    private var primitiveMargin: Int = 0
    private var levelData: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_puzzle)

        val packNumber = intent.getStringExtra("pack")
        supportActionBar?.title = "Pack $packNumber"

        levelData = intent.getStringExtra("level data")

        val grid = findViewById<GridLayout>(R.id.level_grid)
        grid.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                grid.viewTreeObserver.removeOnGlobalLayoutListener(this)
                onLayoutLoad()
            }
        })
    }

    private fun onLayoutLoad() {
        calculateResolution()
        createGrid(levelData)
    }

    private fun calculateResolution() {
        val contentArea = findViewById<LinearLayout>(R.id.select_puzzle_view)

        primitiveSize = (0.195 * contentArea.width).toInt()
        primitiveMargin = ((0.2 / 14) * contentArea.width).toInt()

        horizontalMargin = (4 * (0.2 / 14) * contentArea.width).toInt()
        verticalMargin = (contentArea.height - 6 * primitiveSize - 12 * primitiveMargin) / 2
    }

    private fun createGrid(levelData: String) {
        val grid = findViewById<GridLayout>(R.id.level_grid)

        grid.columnCount = 4
        grid.rowCount = 6

        val gridParams = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.MATCH_PARENT)
        gridParams.setMargins(horizontalMargin, verticalMargin,
            horizontalMargin, verticalMargin)
        grid.layoutParams = gridParams

        for (i in 0 until grid.rowCount)
            for (j in 0 until grid.columnCount){
                val level = Button(this)

                // Set size
                val params = GridLayout.LayoutParams()
                params.width = primitiveSize
                params.height = primitiveSize
                params.setMargins(primitiveMargin, primitiveMargin, primitiveMargin, primitiveMargin)
                level.layoutParams = params

                if (levelData[i * grid.columnCount + j] != 'c') {
                    level.gravity = Gravity.CENTER
                    level.text = (i * grid.columnCount + j + 1).toString()
                    level.textSize = 17.toFloat()

                    level.setTextColor(if (levelData[i * grid.columnCount + j] != 's')
                        Color.WHITE else Color.WHITE)
                    level.setBackgroundColor(if (levelData[i * grid.columnCount + j] != 's')
                        Color.GREEN else Color.YELLOW)
                } else
                    level.setBackgroundColor(Color.BLACK)

                level.tag = i * grid.columnCount + j
                level.setOnClickListener(levelButtonListener)
                grid.addView(level)
            }
    }

    private val levelButtonListener = { v: View ->
        if (levelData[v.tag as Int] != 'c') {
            val intent = Intent(this, Main::class.java).apply {
                putExtra("selected level", v.tag.toString())
            }
            startActivity(intent)
        }
    }
}
