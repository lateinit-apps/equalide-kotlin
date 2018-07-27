package com.braingets.equalide.activities

import android.Manifest
import android.os.Bundle
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager

import android.support.v4.content.ContextCompat
import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity

import android.graphics.Color

import android.view.View
import android.view.Gravity
import android.view.Menu
import android.view.MenuItem
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.widget.*

import kotlinx.android.synthetic.main.main_screen.*

import com.braingets.equalide.R
import com.braingets.equalide.logic.WRITE_PERMISSION_REQUEST

class SelectLevel : AppCompatActivity() {

    // Tile related
    private var rowCount = 0
    private val columnCount = 4
    private var tileSize: Int = 0
    private var levelData: String = ""
    private var grid: GridLayout? = null

    // Margin related
    private var horizontalMargin: Int = 0
    private var verticalMargin: Int = 0
    private var primitiveMargin: Int = 0

    // Activity related
    private var exportIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_select_level)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        grid = findViewById(R.id.level_grid)

        val directoryName = intent.getStringExtra("directory")
        val packName = intent.getStringExtra("pack")
        supportActionBar?.title = "$directoryName/$packName"

        levelData = intent.getStringExtra("level data")
        rowCount = levelData.length / columnCount + (if (levelData.length % columnCount != 0) 1 else 0)

        // Listener to detect when layout is loaded to get it's resolution properties
        grid?.viewTreeObserver?.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                grid?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                calculateViewsSizes()
                createGrid(levelData)
            }
        })
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)

        val exportedFileName = intent?.getStringExtra("exported file name")

        if (exportedFileName != null) {
            Toast.makeText(this, "Exported as $exportedFileName ", Toast.LENGTH_LONG).show()
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

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.right_left_enter, R.anim.right_left_exit)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.select_level_screen_actionbar_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                overridePendingTransition(R.anim.right_left_enter, R.anim.right_left_exit)
            }

            R.id.create_puzzle_button -> {
                val intent = Intent(this, EditPuzzle::class.java)
                    .putExtra("create puzzle", true)

                startActivity(intent)

                overridePendingTransition(R.anim.left_right_enter, R.anim.left_right_exit)
        }

            R.id.export_pack_button -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun calculateViewsSizes() {
        val selectView = findViewById<ScrollView>(R.id.activity_select_level)

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

        loop@ for (i in 0 until rowCount)
            for (j in 0 until columnCount) {
                if (i * columnCount + j == levelData.length)
                    break@loop

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

            val intent = Intent(this, Main::class.java)
                .putExtra("selected level", v.tag as Int)

            setResult(Activity.RESULT_OK, intent)
            finish()

            overridePendingTransition(R.anim.fade_in, R.anim.fade_out)
        }
    }
}
