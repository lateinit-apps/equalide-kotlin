package equalide.kotlin.logic

class Puzzle(text: String, val parts: Int) {
    private val source: String
    private var body: String
    var opened: Boolean
    var solved: Boolean
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

        opened = false
        solved = false
    }

    operator fun get(i: Int, j: Int) : Char {
        return body[i * width + j]
    }

    operator fun set(i: Int, j: Int, c: String) {
        body = body.replaceRange(i * width + j, i * width + j + 1, c)
    }

    fun getPartition() : String {
        return this.body
    }

    fun setPartition(partition: String) {
        this.body = partition
    }

    fun refresh() {
        body = source
    }

    fun checkForSolution() : Boolean {
        if (body.indexOf('w') != -1)
            return false

        val partition = getPartition(body, width)
        if (partition.size != parts)
            return false

        val element = partition[0]
        var result = true

        for (i in 1 until partition.size)
            result = result and (element == partition[i])

        return result
    }

    private fun getPartition(figure: String, width: Int) : ArrayList<Element> {
        val unical = figure.toSet().filter { c: Char -> c != 'b' && c != 'w' }
        val result = ArrayList<Element>()


        for (c in unical) {
            val first = figure.indexOf(c)
            val last = figure.lastIndexOf(c)
            result.add(
                Element(
                    figure
                        .substring(
                            first - first % width,
                            last + width - last % width
                        )
                        .replace("[^$c]".toRegex(), "w")
                        .replace(c, '1'), width
                )
            )
        }
        return result
    }
}