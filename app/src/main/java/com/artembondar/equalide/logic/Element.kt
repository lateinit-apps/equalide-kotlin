package com.artembondar.equalide.logic

// Contains element with next representation:
// 'c' - non-empty cell
// 'e' - empty cell
class Element(private var body: String, var width: Int) {

    var height: Int

    init {
        height = body.length / width
        cutByWidth()
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Element)
            return false
        return this.compare(other)
    }

    override fun hashCode(): Int {
        return body.hashCode()
    }

    // Checks equality to another element with accuracy to rotations and reflections
    private fun compare(other: Element): Boolean {
        var result = this.width == other.width && this.height == other.height &&
                (this.body == other.body || this.mirrorByHeight() == other.body)

        if (!result) {
            for (i in 0 until 3) {
                this.rotateClockWise()
                if (this.width == other.width && this.height == other.height &&
                    (this.body == other.body || this.mirrorByHeight() == other.body)) {
                    result = true
                    break
                }
            }
        }
        return result
    }

    // Cut element to it's bounding rectangle of the same height
    private fun cutByWidth() {
        val startIndexes = ArrayList<Int>()
        val endIndexes = ArrayList<Int>()

        // Gets all starting and ending indexes of non-empty cells on every row
        for (i in 0 until height) {
            for (j in 0 until width) {
                if (body[i * width + j] != 'e') {
                    startIndexes.add(j)
                    break
                }
            }
            for (j in width - 1 downTo 0) {
                if (body[i * width + j] != 'e') {
                    endIndexes.add(j)
                    break
                }
            }
        }

        // Calculate bounds by width
        val start = startIndexes.min() as Int
        val end = endIndexes.max() as Int

        // Perform cutting if possible
        if (start != 0 || end != width - 1) {
            var result = ""

            for (i in 0 until height)
                for (j in start..end)
                    result += body[i * width + j]

            body = result
            width = end - start + 1
            height = body.length / width
        }
    }

    private fun rotateClockWise() {
        var result = ""

        for (i in 0 until width)
            for (j in height - 1 downTo 0)
                result += body[j * width + i]

        body = result
        width = height
        height = body.length / width
    }

    private fun mirrorByHeight(): String {
        var result = ""

        for (i in 0 until height)
            for (j in width - 1 downTo 0)
                result += body[i * width + j]

        return result
    }

    // Checks if element has only one connected component
    fun checkConnectivity(): Boolean {
        // Stores already traversed cell indexes
        val checkedIndexes = mutableSetOf<Int>()

        // Stores pending cell indexes to traverse on next step
        val pendingIndexes = mutableSetOf(body.indexOf('c'))

        // Stores cell indexes that could be traversed after pending cells
        val findedIndexes = mutableSetOf<Int>()

        var result = true

        // Traversing figure starting from selected index (first upper left non-empty cell)
        while (pendingIndexes.size != 0) {
            findedIndexes.clear()

            for (index in pendingIndexes) {
                // Indexes of all neighbour cells
                val up = if (index - width >= 0) index - width else null
                val down = if (index + width < body.length) index + width else null
                val left = if (index % width != 0) index - 1 else null
                val right = if (index % width != width - 1) index + 1 else null

                val indexesForCheck = listOf(up, down, left, right)

                for (i in indexesForCheck)
                    if (i != null && body[i] == 'c' && i !in checkedIndexes)
                        findedIndexes.add(i)
            }
            checkedIndexes.addAll(pendingIndexes)
            pendingIndexes.clear()
            pendingIndexes.addAll(findedIndexes)
        }

        // Checks if element has any non-traversed cells
        for (i in 0 until body.length)
            if (body[i] == 'c' && i !in checkedIndexes) {
                result = false
                break
            }

        return result
    }
}
