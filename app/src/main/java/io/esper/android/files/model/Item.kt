package io.esper.android.files.model

import java.util.*

@Suppress("unused")
class Item : Comparable<Item?> {
    var name: String? = null
    var data: String? = null
    var date: String? = null
    var path: String? = null
    private var image: String? = null
    var emptySubFolder: Boolean = false
    var isDirectory = false
    var size: String? = null

    constructor()
    constructor(
        name: String?,
        data: String?,
        date: String?,
        path: String?,
        image: String?,
        emptySubFolder: Boolean,
        isDirectory: Boolean,
        size: String
    ) {
        this.name = name
        this.data = data
        this.date = date
        this.path = path
        this.image = image
        this.emptySubFolder = emptySubFolder
        this.isDirectory = isDirectory
        this.size = size
    }

    override fun compareTo(other: Item?): Int {
        return if (name != null) {
            name!!.toLowerCase(Locale.getDefault())
                .compareTo(other!!.name!!.toLowerCase(Locale.getDefault()))
        } else {
            throw IllegalArgumentException()
        }
    }
}