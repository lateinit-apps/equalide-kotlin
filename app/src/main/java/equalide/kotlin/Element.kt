package equalide.kotlin

class Element(private var body: String, var width: Int) {
    var height: Int

    init {
        height = body.length / width
        cutByWidth()
    }

    private fun cutByWidth() {
        val startIndexes = ArrayList<Int>()
        val endIndexes = ArrayList<Int>()

        for (i in 0 until body.length / width) {
            for (j in 0 until width) {
                if (body[i * width + j] != 'w') {
                    startIndexes.add(j)
                    break
                }
            }
            for (j in width - 1 downTo 0) {
                if (body[i * width + j] != 'w') {
                    endIndexes.add(j)
                    break
                }
            }
        }

        val start = startIndexes.min() as Int
        val end = endIndexes.max() as Int
        var result = ""

        for (i in 0 until body.length / width)
            for (j in start..end)
                result += body[i * width + j]

        body = result
        width = end - start + 1
        height = body.length / width
    }
}