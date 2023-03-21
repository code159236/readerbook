package com.v2reading.reader.data.entities

import android.content.Context
import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.v2reading.reader.R
import com.v2reading.reader.constant.AppConst
import kotlinx.parcelize.Parcelize

@Parcelize
@Entity(tableName = "book_groups")
data class BookGroup(
    @PrimaryKey
    val groupId: Long = 0b1,
    var groupName: String,
    var cover: String? = null,
    var order: Int = 0,
    var show: Boolean = true
) : Parcelable {

    fun getManageName(context: Context): String {
        return when (groupId) {
            AppConst.bookGroupAllId -> "$groupName(${context.getString(R.string.all)})"
            AppConst.bookGroupAudioId -> "$groupName(${context.getString(R.string.audio)})"
            AppConst.bookGroupLocalId -> "$groupName(${context.getString(R.string.local)})"
            AppConst.bookGroupNoneId -> "$groupName(${context.getString(R.string.no_group)})"
            else -> groupName
        }
    }

    override fun hashCode(): Int {
        return groupId.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is BookGroup) {
            return other.groupId == groupId
                    && other.groupName == groupName
                    && other.cover == cover
                    && other.order == order
                    && other.show == show
        }
        return false
    }

}