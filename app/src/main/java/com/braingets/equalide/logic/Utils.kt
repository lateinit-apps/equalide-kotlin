package com.braingets.equalide.logic

// Puzzle related
const val DEFAULT_DIRECTORY_INDEX = 0
const val DEFAULT_PACK_INDEX = 0
const val UNSET_DIRECTORY_ID = -1
const val NO_LEVEL_OPENED = -1

// Activity requests
enum class Request(val code: Int) {
    READ_PERMISSION(0),
    WRITE_PERMISSION(1),
    SELECT_LEVEL(2),
    EXPORT_PACK(3)
}

// Launch select level screen launch modes
enum class LaunchMode {
    FOR_LEVEL_SELECT, AFTER_PACK_EXPORT, AFTER_PUZZLE_CREATION
}

// Edit edges buttons direction
enum class Direction {
    UP, DOWN, LEFT, RIGHT, NO_DIRECTION
}

// Edit edges buttons modes
const val INCREASE = 0
const val DECREASE = 1

// Edit edges functions
fun increaseBySide(text: String, isPartition: Boolean, width: Int, direction: Direction): String {
    val emptyChar = if (isPartition) "e" else "1"
    val separator = if (isPartition) "" else "\n"
    val chunkSize = width + if (isPartition) 0 else 1

    return when (direction) {
        Direction.UP -> emptyChar.repeat(width) + separator + text
        Direction.DOWN -> text + separator + emptyChar.repeat(width)
        Direction.LEFT -> (text.chunked(chunkSize)
            .map { line -> emptyChar + line }).joinToString("")

        Direction.RIGHT -> (text.chunked(chunkSize)
            .map { line ->
                if (isPartition || (!isPartition && line.length != chunkSize)) line + emptyChar
                else line.substringBeforeLast("\n") + emptyChar + "\n"
            })
            .joinToString("")

        Direction.NO_DIRECTION -> text
    }
}

fun decreaseBySide(text: String, isPartition: Boolean, width: Int, height: Int, direction: Direction): String? {
    val chunkSize = width + if (isPartition) 0 else 1

    return when (direction) {
        Direction.UP -> if (height == 1) null else
            text.substring(width + (if (isPartition) 0 else 1))

        Direction.DOWN -> if (height == 1) null else
            text.substring(0, text.lastIndex - width + (if (isPartition) 1 else 0))

        Direction.LEFT -> if (width == 1) null else
            (text.chunked(chunkSize)
                .map { it.substring(1) }).joinToString("")

        Direction.RIGHT -> if (width == 1) null else
            (text.chunked(chunkSize)
                .map {
                    if (isPartition || (!isPartition && it.length != chunkSize)) it.substring(0, it.lastIndex)
                    else it.substring(0, it.lastIndex - 1) + "\n"
                })
                .joinToString("")

        Direction.NO_DIRECTION -> text
    }
}

fun normalizePuzzleSource(source: String): String {
    val unicalNumbers = source.toSet().filter { c -> c != 'b' && c != '\n'}

    return source.map { c ->
        if (c != 'b' && c != '\n') (unicalNumbers.indexOf(c) + 1).toString()
        else c.toString()
    }
        .joinToString("")
}

private val intPairComparator = Comparator { o1: Pair<Int, Int>, o2: Pair<Int, Int> ->
    if (o1.first > o2.first ||
        (o1.first == o2.first && o1.second > o2.second)) 1
    else if (o1.first == o2.first && o1.second == o2.second) 0
    else -1
}

// Cut puzzle string to it's bounding rectangle
fun cutByBoundingRectangle(source: String): String {
    val startIndexes = ArrayList<Pair<Int, Int>>()
    val endIndexes = ArrayList<Pair<Int, Int>>()

    var puzzle = source.lines()
    val height = puzzle.size
    val width = puzzle[0].length

    // Gets all starting and ending indexes of non-empty cells on every row
    for (i in 0 until height) {
        for (j in 0 until width) {
            if (puzzle[i][j] != '0') {
                startIndexes.add(Pair(i, j))
                break
            }
        }
        for (j in width - 1 downTo 0) {
            if (puzzle[i][j] != '0') {
                endIndexes.add(Pair(i, j))
                break
            }
        }
    }

    // Calculate bounds
    val start = startIndexes.minWith(intPairComparator)
    val end = endIndexes.maxWith(intPairComparator)

    // Perform cutting if possible
    if (start != null && end != null) {
        puzzle = puzzle.subList(start.first, end.first + 1)
        puzzle = puzzle.map { it.substring(start.second, end.second + 1) }
    }

    return puzzle.joinToString("\n")
}
