package com.braingets.equalide.activities

import android.os.Bundle

import android.support.v7.app.AppCompatActivity

import kotlinx.android.synthetic.main.main_screen.*

import com.braingets.equalide.R

class EditPuzzle : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_edit_puzzle)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}
