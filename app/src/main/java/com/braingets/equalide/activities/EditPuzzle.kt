package com.braingets.equalide.activities

import android.os.Bundle
import android.content.Intent

import android.support.v7.app.AppCompatActivity

import android.view.Menu
import android.view.MenuItem

import kotlinx.android.synthetic.main.main_screen.*

import com.braingets.equalide.R

class EditPuzzle : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_puzzle)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
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

            }

            R.id.save_puzzle_button -> {

            }

            R.id.edit_edges_button -> {

            }
        }
        return super.onOptionsItemSelected(item)
    }
}
