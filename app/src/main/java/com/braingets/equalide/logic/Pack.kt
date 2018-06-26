package com.braingets.equalide.logic

class Pack(val puzzles: Array<Puzzle>) {

    var opened: Boolean = false
    var solved: Boolean = false

    fun checkIfSolved(): Boolean {
        for (i in 0 until puzzles.size)
            if (!puzzles[i].solved)
                return false

        return true
    }
}
