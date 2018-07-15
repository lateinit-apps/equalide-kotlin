package com.braingets.equalide.data

import com.braingets.equalide.logic.Pack

typealias Directory = MutableList<Pack>

class LevelData {

    private var directories = mutableListOf<Directory>()
    private var names = mutableListOf<String>()

    fun addDirectory(directory: Directory, name: String) {
        directories.add(directory)
        names.add(name)
    }

    fun removeDirectory(index: Int) {
        if (index >= 0 && index < directories.size) {
            directories.removeAt(index)
            names.removeAt(index)
        }
    }
}
