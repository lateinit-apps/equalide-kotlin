package com.lateinit.apps.equalide.logic

import com.lateinit.apps.equalide.activities.Main.CurrentPuzzle

class LevelData {

    private var directories = mutableListOf<Directory>()
    private var directoriesHashes = mutableListOf<Int>()
    var size: Int = 0
        get() = directories.size
        private set

    fun add(directory: Directory) {
        if (directory.hash == UNSET_DIRECTORY_ID) {
            val hash = generateDirectoryHash()

            directory.changeHash(hash)
            directoriesHashes.add(hash)
        } else
            directoriesHashes.add(directory.hash)

        directories.add(directory)
    }

    fun removeDirectory(index: Int) = directories.removeAt(index)

    private fun generateDirectoryHash(): Int {
        var hash = 0

        while (hash in directoriesHashes)
            hash++

        return hash
    }

    operator fun get(i: Int) = directories[i]

    operator fun get(puzzleIndex: CurrentPuzzle) = directories[puzzleIndex.directory][puzzleIndex.pack][puzzleIndex.number]

    operator fun set(puzzleIndex: CurrentPuzzle, puzzle: Puzzle) {
        directories[puzzleIndex.directory][puzzleIndex.pack][puzzleIndex.number] = puzzle
    }

    operator fun iterator(): Iterator<Directory> = directories.iterator()
}
