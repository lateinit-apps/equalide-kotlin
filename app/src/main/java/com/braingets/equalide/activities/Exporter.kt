package com.braingets.equalide.activities

import java.io.File
import java.io.FileOutputStream

import android.app.IntentService
import android.content.Intent
import android.os.Environment

class Exporter : IntentService("exporter") {

    override fun onHandleIntent(intent: Intent?) {
        val text = intent?.getStringExtra("text")
        val fileName = intent?.getStringExtra("file name")

        if (text != null && fileName != null)
            export(text, fileName)
    }

    private fun export(text: String, fileName: String) {
        val directory = android.os.Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        var file = File(directory, "")

        var i = 1
        while (file.exists()) {
            file = File(directory, "$fileName ($i)")
            i++
        }

        val outputStream = FileOutputStream(file)
        outputStream.write(text.toByteArray())
        outputStream.close()
    }
}
