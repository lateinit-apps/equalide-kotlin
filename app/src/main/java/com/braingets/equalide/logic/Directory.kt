package com.braingets.equalide.logic

class Directory(val name: String) {
    private val packs = mutableListOf<Pack>()
    private val packIds = mutableListOf<Int>()
    var id: Int = UNSET_DIRECTORY_ID
        private set
    var size: Int = 0
        get() = packs.size
        private set

    constructor(name: String, directoryId: Int) : this(name) {
        id = directoryId
    }

    fun add(pack: Pack, id: Int = generatePackId()) {
        packs.add(pack)
        packIds.add(id)
    }

    fun removePack(index: Int, default: Boolean) {
        if (default && index == DEFAULT_PACK_INDEX)
             packs[DEFAULT_PACK_INDEX] = Pack(mutableListOf())
        else {
            packs.removeAt(index)
            packIds.removeAt(index)
        }
    }

    private fun generatePackId(): Int {
        var id = 0

        while (id in packIds)
            id++

        return id
    }

    fun changeId(newId: Int) {
        id = newId
    }

    operator fun get(i: Int) = packs[i]

    fun getPackId(i: Int) = packIds[i]

    operator fun iterator() : Iterator<Pack> = packs.iterator()

    fun clear(default: Boolean) {
        packs.clear()
        packIds.clear()

        if (default)
            add(Pack(mutableListOf()))
    }
}
