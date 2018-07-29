package com.braingets.equalide.logic

// Puzzle related
const val DEFAULT_DIRECTORY_INDEX = 0
const val DEFAULT_PACK_INDEX = 0
const val UNSET_DIRECTORY_ID = -1
const val NO_LEVEL_OPENED = -1

// Activity related
const val READ_PERMISSION_REQUEST = 1
const val WRITE_PERMISSION_REQUEST = 2
const val SELECT_LEVEL_REQUEST = 3
const val EXPORT_PACK_REQUEST = 4

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
