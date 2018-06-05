package equalide.kotlin

class Pack(val puzzles: Array<Puzzle>) {
    var opened: Boolean
    var solved: Boolean

    init {
        opened = false
        solved = false
    }
}