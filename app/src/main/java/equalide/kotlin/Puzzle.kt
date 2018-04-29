package equalide.kotlin

class Puzzle {
    val width: Int
    val height: Int
    val parts: Int
    val body: Array<IntArray>

    constructor(text: String) {
        val array = text.split("\n", "\r\n")
        height = array.size
        width = array[0].length
        body = Array(height, { IntArray(width)})
        var max = 0
        for (i in 0..array.size - 1)
            for (j in 0..width - 1){
                body[i][j] = array[i][j].toInt() - 48
                if (body[i][j] > max) max = body[i][j]
        }
        parts = max
    }
}