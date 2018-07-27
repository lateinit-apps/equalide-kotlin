package com.braingets.equalide.activities

import android.Manifest
import android.os.Bundle
import android.content.Intent
import android.content.pm.PackageManager

import android.support.v4.app.ActivityCompat
import android.support.v7.app.AppCompatActivity

import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

import kotlinx.android.synthetic.main.main_screen.*

import com.braingets.equalide.R
import com.braingets.equalide.logic.Puzzle
import com.braingets.equalide.logic.WRITE_PERMISSION_REQUEST

class EditPuzzle : AppCompatActivity() {

    private var puzzle: Puzzle? = null
    private var exportIntent: Intent? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_puzzle)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val createPuzzle = intent.getBooleanExtra("create puzzle", false)
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

            R.id.export_puzzle_button -> {
                val localClassName = localClassName.split(".")
                val className = localClassName[localClassName.lastIndex]

                val intent = Intent(this, Exporter::class.java)
                    .putExtra("text", puzzle?.source)
                    .putExtra("file name", "puzzle.eqld")
                    .putExtra("class name", className)
                startService(intent)
            }

            R.id.save_puzzle_button -> {

            }

            R.id.edit_edges_button -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }
}
