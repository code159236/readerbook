package com.v2reading.reader.data.entities.rule

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class RowUi(
    var name: String,
    var type: String?,
    var action: String?
) : Parcelable