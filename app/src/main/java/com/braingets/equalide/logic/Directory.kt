package com.braingets.equalide.logic

class Directory(val name: String) {
    private val packs = mutableListOf<Pack>()
    private val packsId = mutableListOf<Int>()
    val id: Int = generateViewId()
    var size: Int = 0
        get() = packs.size
        private set

    constructor(name: String, packs: MutableList<Pack>) : this(name) {
        this.packs.addAll(packs)

        for (pack in packs)
            packsId.add(generateViewId())
    }

    fun add(pack: Pack) {
        packs.add(pack)
        packsId.add(generateViewId())
    }

    operator fun get(i: Int) = packs[i]

    fun getPackId(i: Int) = packsId[i]

    operator fun iterator() : Iterator<Pack> = packs.iterator()
}
