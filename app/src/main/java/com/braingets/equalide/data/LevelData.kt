package com.braingets.equalide.data

import com.braingets.equalide.logic.Directory

class LevelData {

    private var directories = mutableListOf<Directory>()
    var size: Int = 0
        get() = directories.size
        private set

    fun add(directory: Directory) = directories.add(directory)

    fun removeDirectory(index: Int) = directories.removeAt(index)

    operator fun get(i: Int) = directories[i]
}
