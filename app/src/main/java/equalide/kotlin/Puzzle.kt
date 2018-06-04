package equalide.kotlin

import android.util.Log

class Puzzle(text: String, val parts: Int) {
    private val source: String
    var body: String
    val width: Int
    val height: Int

    init {
        val array = text.split("\n", "\r\n")
        height = array.size
        width = array[0].length

        source = array.joinToString("")
            .replace('0', 'b')
            .replace('1', 'w')
        body = source
    }

    operator fun get(i: Int, j: Int) : Char {
        return body[i * width + j]
    }

    operator fun set(i: Int, j: Int, c: Char) {
        body = body.replaceRange(i * width + j, i * width + j, c.toString())
    }

    fun checkForSolution() : Boolean {
       return false
    }

    fun refresh() {
    }
}