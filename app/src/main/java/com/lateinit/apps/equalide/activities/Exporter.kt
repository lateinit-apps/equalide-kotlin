package com.lateinit.apps.equalide.activities

import java.io.File
import java.io.FileOutputStream

import android.Manifest
import android.os.Environment
import android.app.IntentService
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

class Exporter : IntentService("exporter") {

    private var callerClass: Class<*>? = null

    override fun onHandleIntent(intent: Intent?) {
        val text = intent?.getStringExtra("text")
        val fileName = intent?.getStringExtra("file name")
        val callerClassName = intent?.getStringExtra("class name")

        if (text != null && fileName != null && callerClassName != null) {
            when (callerClassName) {
                "Main" -> callerClass = Main::class.java
                "SelectLevel" -> callerClass = SelectLevel::class.java
                "EditPuzzle" -> callerClass = EditPuzzle::class.java
            }

            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {

                intent.setClass(this, callerClass!!)
                intent.putExtra("no write permission", true)
                startActivity(intent)

                this.stopSelf()
            } else
                export(text, fileName)
        }
    }

    private fun export(text: String, fileName: String) {
        val directory = android.os.Environment
            .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).absolutePath
        var file = File(directory, fileName)

        var exportedFileName = fileName

        if (file.exists()) {
            val filePath = fileName.split(".")

            val name = filePath[0]
            var extension = ""

            if (filePath.size > 1)
                extension = "." + filePath[filePath.lastIndex]

            var i = 1
            while (file.exists()) {
                exportedFileName = "$name ($i)$extension"

                file = File(directory, exportedFileName)
                i++
            }
        }

        val outputStream = FileOutputStream(file)
        outputStream.write(text.toByteArray())
        outputStream.close()

        val intent = Intent(this, callerClass)
            .putExtra("exported file name", exportedFileName)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK

        startActivity(intent)
    }
}
