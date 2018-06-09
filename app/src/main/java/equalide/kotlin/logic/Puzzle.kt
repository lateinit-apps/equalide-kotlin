package equalide.kotlin.logic

// Contains puzzle with next representation:
// '0-9' - colored cell
// 'e' - empty cell, can be colored
// 'b' - blank cell, can't be colored
class Puzzle(text: String, val parts: Int) {

    private val source: String
    private var body: String

    val width: Int
    val height: Int

    var opened: Boolean
    var solved: Boolean

    init {
        val lines = text.lines()

        height = lines.size
        width = lines[0].length

        source = lines.joinToString("")
            .replace('0', 'b')
            .replace('1', 'e')
        body = source

        opened = false
        solved = false
    }

    operator fun get(i: Int, j: Int): Char {
        return body[i * width + j]
    }

    operator fun set(i: Int, j: Int, c: String) {
        body = body.replaceRange(i * width + j, i * width + j + 1, c)
    }

    fun getPartition(): String {
        return this.body
    }

    fun setPartition(partition: String) {
        this.body = partition
    }

    fun refresh() {
        body = source
    }

    fun checkIfSolved(): Boolean {
        // Checks if puzzle contains any unpainted primitive
        if (body.indexOf('e') != -1)
            return false

        val elements = separateInElements()
        if (elements.size != parts)
            return false

        // Checks if elements are equal
        for (i in 1 until elements.size)
            if (elements[0] != elements[i])
                return false

        if (!elements[0].checkConnectivity())
            return false

        return true
    }

    private fun separateInElements(): ArrayList<Element> {
        val unicalCells = body.toSet().filter { c: Char -> c != 'b' && c != 'e' }
        val result = ArrayList<Element>()

        for (cell in unicalCells) {
            val firstOccurance = body.indexOf(cell)
            val lastOccurance = body.lastIndexOf(cell)

            result.add(
                Element(
                    body.substring(
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
}
