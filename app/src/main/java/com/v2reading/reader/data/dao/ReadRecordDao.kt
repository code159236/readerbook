package com.v2reading.reader.data.dao

import androidx.room.*
import com.v2reading.reader.data.entities.ReadRecord
import com.v2reading.reader.data.entities.ReadRecordShow

@Dao
interface ReadRecordDao {

    @get:Query("select * from readRecord")
    val all: List<ReadRecord>

    @get:Query("select bookName, sum(readTime) as readTime from readRecord group by bookName order by bookName collate localized")
    val allShow: List<ReadRecordShow>

    @get:Query("select sum(readTime) from readRecord")
    val allTime: Long

    @Query("select sum(readTime) from readRecord where bookName = :bookName")
    fun getReadTime(bookName: String): Long?

    @Query("select readTime from readRecord where deviceId = :androidId and bookName = :bookName")
    fun getReadTime(androidId: String, bookName: String): Long?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insert(vararg readRecord: ReadRecord)

    @Update
    fun update(vararg record: ReadRecord)

    @Delete
    fun delete(vararg record: ReadRecord)

    @Query("delete from readRecord")
    fun clear()

    @Query("delete from readRecord where bookName = :bookName")
    fun deleteByName(bookName: String)
}