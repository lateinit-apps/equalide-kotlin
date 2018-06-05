package equalide.kotlin.logic

class Element(private var body: String, var width: Int) {
    var height: Int

    init {
        height = body.length / width
        cutByWidth()
    }

    private fun cutByWidth() {
        val startIndexes = ArrayList<Int>()
        val endIndexes = ArrayList<Int>()

        for (i in 0 until height) {
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

        for (i in 0 until height)
            for (j in start..end)
                result += body[i * width + j]

        body = result
        width = end - start + 1
        height = body.length / width
    }

    private fun rotateClockWise() {
        var result = ""

        for (i in 0 until width)
            for (j in height - 1 downTo 0)
                result += body[j * width + i]
        body = result
        width = height
        height = body.length / width
    }

    private fun mirrorByHeight() : String {
        var result = ""

        for (i in 0 until height)
            for (j in width - 1 downTo 0)
                result += body[i * width + j]
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || other !is Element)
            return false
        return this.compare(other)
    }

    private fun compare(other: Element) : Boolean {
        var result = this.width == other.width && this.height == other.height &&
                (this.body == other.body || this.mirrorByHeight() == other.body)

        if (!result) {
            for (i in 0 until 3) {
                this.rotateClockWise()
                if (this.width == other.width && this.height == other.height &&
                    (this.body == other.body || this.mirrorByHeight() == other.body)) {
                    result = true
                    break
                }
            }
        }
        return result
    }
}