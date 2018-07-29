package com.braingets.equalide.logic

import com.braingets.equalide.activities.Main.CurrentPuzzle

class LevelData {

    private var directories = mutableListOf<Directory>()
    private var directoriesIds = mutableListOf<Int>()
    var size: Int = 0
        get() = directories.size
        private set

    fun add(directory: Directory) {
        if (directory.id == UNSET_DIRECTORY_ID) {
            val id = generateDirectoryId()

            directory.changeId(id)
            directoriesIds.add(id)
        } else
            directoriesIds.add(directory.id)

        directories.add(directory)
    }

    fun removeDirectory(index: Int) = directories.removeAt(index)

    private fun generateDirectoryId(): Int {
        var id = 0

        while (id in directoriesIds)
            id++

        return id
    }

    operator fun get(i: Int) = directories[i]

    operator fun get(puzzleIndex: CurrentPuzzle) = directories[puzzleIndex.directory][puzzleIndex.pack][puzzleIndex.number]

    operator fun set(puzzleIndex: CurrentPuzzle, puzzle: Puzzle) {
        directories[puzzleIndex.directory][puzzleIndex.pack][puzzleIndex.number] = puzzle
    }

    operator fun iterator(): Iterator<Directory> = directories.iterator()
}
