package com.lateinit.apps.equalide.logic

class Directory(val name: String) {
    private val packs = mutableListOf<Pack>()
    private val packHashes = mutableListOf<Int>()
    var hash: Int = UNSET_DIRECTORY_ID
        private set
    var size: Int = 0
        get() = packs.size
        private set

    constructor(name: String, directoryHash: Int) : this(name) {
        hash = directoryHash
    }

    fun add(pack: Pack, hash: Int = generatePackHash()) {
        packs.add(pack)
        packHashes.add(hash)
    }

    fun removePack(index: Int, default: Boolean) {
        if (default && index == DEFAULT_PACK_INDEX)
             packs[DEFAULT_PACK_INDEX] = Pack(mutableListOf())
        else {
            packs.removeAt(index)
            packHashes.removeAt(index)
        }
    }

    private fun generatePackHash(): Int {
        var hash = 0

        while (hash in packHashes)
            hash++

        return hash
    }

    fun changeHash(newHash: Int) {
        hash = newHash
    }

    operator fun get(i: Int) = packs[i]

    fun getPackHash(i: Int) = packHashes[i]

    operator fun iterator(): Iterator<Pack> = packs.iterator()

    fun clear(default: Boolean) {
        packs.clear()
        packHashes.clear()

        if (default) {
            add(Pack(mutableListOf()))
            packs[0].opened = true
        }
    }
}
