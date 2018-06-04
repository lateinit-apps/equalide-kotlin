package equalide.kotlin

import android.util.Log

class Puzzle(text: String, val parts: Int) {
    private val source: String
    private var body: String
    private var solved: Boolean
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

        solved = false
    }

    operator fun get(i: Int, j: Int) : Char {
        return body[i * width + j]
    }

    operator fun set(i: Int, j: Int, c: String) {
        body = body.replaceRange(i * width + j, i * width + j + 1, c)
    }

    fun refresh() {
        body = source
    }

    fun checkForSolution() : Boolean {
        val partition = getPartition(body, 4)

       return false
    }

    private fun getPartition(figure: String, width: Int) : ArrayList<Element> {
        val figure1 =
            "ww2w" + "3221" + "w111" + "w333" + "w3ww"

        val unical = figure1.toSet().filter { c: Char -> c != 'b' && c != 'w' }
        val result = ArrayList<Element>()


        for (c in unical) {
            val first = figure1.indexOf(c)
            val last = figure1.lastIndexOf(c)
            result.add(Element(
                figure1.substring(
                    first - first % width,
                    last + (if (last % width != 0) width - 1 - last % width else 0) + 1
                ).replace("[^$c]".toRegex(), "w"), width)
            )
        }
        return result
    }
}