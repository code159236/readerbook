package com.v2reading.reader.lib.dialogs

@Suppress("unused")
data class SelectItem<T>(
    val title: String,
    val value: T
) {

    override fun toString(): String {
        return title
    }

}
