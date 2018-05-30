package equalide.kotlin

import android.util.Log

class Puzzle(text: String) {
    val width: Int
    val height: Int
    val parts: Int
    val body: Array<IntArray>
    val solution: String
    val solved: Boolean

    init {
        solved = false
        val array = text.split("\n", "\r\n")
        solution = array.joinToString("")
        height = array.size
        width = array[0].length
        body = Array(height, { IntArray(width)})
        var max = 0
        for (i in 0 until array.size)
            for (j in 0 until width){
                val digit = array[i][j].toInt() - 48
                if (digit > max) max = digit
                body[i][j] = if (digit == 0) -2 else -1
        }
        parts = max
    }

    fun checkForSolution() : Boolean {
        var string = ""
        var translation = "0"

        for (i in 0 until body.size) {
            for (j in 0 until body[0].size) {
                string += if (body[i][j] >= 0) (body[i][j] + 1).toString() else "0"
            }
        }

        for (i in 1..parts) {
            var index = solution.indexOf(i.toString())
            var x = index / body[0].size
            var y = index % body[0].size
//            Log.d("TRANSLATIONX", x.toString())
//            Log.d("TRANSLATIONY", y.toString())
//            Log.d("TRANSLATION", index.toString())
            translation += (body[x][y] + 1).toString()
        }

        var translated = string.map({ c: Char ->
            translation.indexOf(c)
        }).joinToString("")

//        Log.d("TRANSLATION_SOL", solution)
//        Log.d("TRANSLATION_OR_", string)
//        Log.d("TRANSLATION____", translated)
//        Log.d("TRANSLATION_AL", translation)

        return solution.compareTo(translated) == 0
    }

    fun refresh() {
        for (i in 0 until body.size) {
            for (j in 0 until body[0].size) {
                body[i][j] = if (body[i][j] >= 0) -1 else body[i][j]
            }
        }
    }
}