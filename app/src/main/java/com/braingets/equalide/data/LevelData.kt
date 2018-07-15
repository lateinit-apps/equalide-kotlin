package com.braingets.equalide.data

import com.braingets.equalide.logic.Pack

typealias Directory = Array<Pack>

class LevelData() {
    private var directories = mutableListOf<Directory>()

    fun addDirectory(directory: Directory) = directories.add(directory)

    fun removeDirectory(index: Int) {
        if (index >= 0 && index < directories.size)
            directories.removeAt(index)
    }
}
