package com.braingets.equalide.logic

class Pack(private val puzzles: MutableList<Puzzle>) {

    var opened: Boolean = false
    var solved: Boolean = false
    var size: Int = 0
        get() = puzzles.size
        private set

    operator fun get(i: Int) = puzzles[i]

    operator fun set(i: Int, puzzle: Puzzle) {
        puzzles[i] = puzzle
    }

    operator fun iterator(): Iterator<Puzzle> = puzzles.iterator()

    fun checkIfSolved(): Boolean {
        for (i in 0 until puzzles.size)
            if (!puzzles[i].solved)
                return false

        return true
    }

    fun addAll(puzzles: MutableList<Puzzle>) = this.puzzles.addAll(puzzles)
}
