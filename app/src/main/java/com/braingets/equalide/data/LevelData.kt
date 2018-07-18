package com.braingets.equalide.data

import com.braingets.equalide.logic.Pack

typealias Directory = MutableList<Pack>

class LevelData {

    private var directories = mutableListOf<Directory>()
    private var names = mutableListOf<String>()
    var size = 0
        private set

    fun addDirectory(directory: Directory, name: String) {
        directories.add(directory)
        names.add(name)
        size++
    }

    fun removeDirectory(index: Int) {
        directories.removeAt(index)
        names.removeAt(index)
        size--
    }

    operator fun get(i: Int) = directories[i]

    fun name(i: Int) = names[i]
}
