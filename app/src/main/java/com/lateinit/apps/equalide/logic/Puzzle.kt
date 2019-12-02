package com.lateinit.apps.equalide.logic

// Contains puzzle with next representation:
// '0-9' - colored cell
// 'e' - empty cell, can be colored
// 'b' - blank cell, can't be colored
class Puzzle(val source: String) {

    private val cleaned: String

    val solution: String
    val parts: Int

    val width: Int
    val height: Int

    var partition: String
        private set
    var opened: Boolean
    var solved: Boolean

    init {
        parts = source.toSet().filter { c -> c != '0' && c != '\n' }.size

        val lines = source.lines()

        height = lines.size
        width = lines[0].length

        solution = lines.joinToString("").replace('0', 'b')
            .replace(Regex("[1-9]")) { c -> (c.value.toInt() - 1).toString() }
        cleaned = solution.replace(Regex("[0-9]"), "e")
        partition = cleaned

        opened = false
        solved = false
    }

    operator fun get(i: Int, j: Int) = partition[i * width + j]

    operator fun set(i: Int, j: Int, c: String) {
        partition = partition.replaceRange(i * width + j, i * width + j + 1, c)
    }

    fun loadPartition(partition: String) {
        this.partition = partition
    }

    fun refresh() {
        partition = cleaned
    }

    fun checkIfSolved(): Boolean {
        // Checks if puzzle contains any unpainted primitive
        if (partition.indexOf('e') != -1)
            return false

        val elements = separateInElements()

        if (elements.size != parts)
            return false

        if (!elements[0].checkConnectivity())
            return false

        // Checks if elements are equal
        for (i in 1 until elements.size)
            if (elements[0] != elements[i])
                return false

        return true
    }

    fun checkIfValid(): Boolean {
        // Checks if puzzle contains any unpainted primitive
        if (partition.indexOf('e') != -1)
            return false

        val elements = separateInElements()

        if (elements.size == 1)
            return false

        if (!elements[0].checkConnectivity())
            return false

        // Checks if elements are equal
        for (i in 1 until elements.size)
            if (elements[0] != elements[i])
                return false

        return true
    }

    private fun separateInElements(): ArrayList<Element> {
        val unicalCells = partition.toSet().filter { c -> c != 'b' && c != 'e' }
        val result = ArrayList<Element>()

        for (cell in unicalCells) {
            val firstOccurance = partition.indexOf(cell)
            val lastOccurance = partition.lastIndexOf(cell)

            result.add(
                Element(
                    partition.substring(
                        firstOccurance - firstOccurance % width,
                        lastOccurance + width - lastOccurance % width
                    )
                        .replace(Regex("[^$cell]"), "e")
                        .replace(cell, 'c'), width
                )
            )
        }
        return result
    }

    fun getChangedBySide(direction: Direction, mode: Int): Puzzle {
        var newSource: String? = null
        var newPartition: String? = null

        when (mode) {
            INCREASE -> {
                newSource = increaseBySide(source, false, width, direction)
                newPartition = increaseBySide(partition, true, width, direction)
            }
            DECREASE -> {
                newSource = decreaseBySide(source, false, width, height, direction)
                newPartition = decreaseBySide(partition, true, width, height, direction)
            }
        }

        val newPuzzle = if (newSource != null) Puzzle(newSource) else Puzzle("1")
        if (newPartition != null)
            newPuzzle.loadPartition(newPartition)

        return newPuzzle
    }
}
