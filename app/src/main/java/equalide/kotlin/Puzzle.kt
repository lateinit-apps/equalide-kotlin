package equalide.kotlin

class Puzzle(text: String) {
    val width: Int
    val height: Int
    val parts: Int
    val body: Array<IntArray>

    init {
        val array = text.split("\n", "\r\n")
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
}