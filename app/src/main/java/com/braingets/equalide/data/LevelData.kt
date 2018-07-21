package com.braingets.equalide.data

import com.braingets.equalide.logic.Directory

class LevelData {

    private var directories = mutableListOf<Directory>()
    private var directoriesIds = mutableListOf<Int>()
    var size: Int = 0
        get() = directories.size
        private set

    fun add(directory: Directory) {
        val id = generateDirectoryId()

        directory.changeId(id)
        directories.add(directory)
        directoriesIds.add(id)
    }

    fun removeDirectory(index: Int) = directories.removeAt(index)

    private fun generateDirectoryId(): Int {
        var id = 0

        while (id in directoriesIds)
            id++

        return id
    }

    operator fun get(i: Int) = directories[i]
}
