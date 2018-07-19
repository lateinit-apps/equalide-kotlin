package com.braingets.equalide.logic

class Directory(val name: String) {
    private val packs = mutableListOf<Pack>()
    val id: Int = generateViewId()
    var size: Int = 0
        get() = packs.size
        private set

    constructor(name: String, packs: MutableList<Pack>) : this(name) {
        this.packs.addAll(packs)
    }

    fun add(pack: Pack) = packs.add(pack)

    operator fun get(i: Int) = packs[i]

    operator fun iterator() : Iterator<Pack> = packs.iterator()
}
