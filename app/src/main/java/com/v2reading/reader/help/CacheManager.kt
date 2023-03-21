package com.v2reading.reader.help

import androidx.collection.LruCache
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.Cache
import com.v2reading.reader.utils.ACache
import splitties.init.appCtx

@Suppress("unused")
object CacheManager {

    private val queryTTFMap = hashMapOf<String, Pair<Long, com.v2reading.reader.model.analyzeRule.QueryTTF>>()
    private val memoryLruCache = object : LruCache<String, String>(100) {}

    /**
     * saveTime 单位为秒
     */
    @JvmOverloads
    fun put(key: String, value: Any, saveTime: Int = 0) {
        val deadline =
            if (saveTime == 0) 0 else System.currentTimeMillis() + saveTime * 1000
        when (value) {
            is com.v2reading.reader.model.analyzeRule.QueryTTF -> queryTTFMap[key] = Pair(deadline, value)
            is ByteArray -> ACache.get(appCtx).put(key, value, saveTime)
            else -> {
                val cache = Cache(key, value.toString(), deadline)
                putMemory(key, value.toString())
                appDb.cacheDao.insert(cache)
            }
        }
    }

    fun putMemory(key: String, value: String) {
        memoryLruCache.put(key, value)
    }

    //从内存中获取数据 使用lruCache
    fun getFromMemory(key: String): String? {
        return memoryLruCache.get(key)
    }

    fun deleteMemory(key: String) {
        memoryLruCache.remove(key)
    }

    fun get(key: String): String? {
        getFromMemory(key)?.let {
            return it
        }
        val cache = appDb.cacheDao.get(key)
        if (cache != null && (cache.deadline == 0L || cache.deadline > System.currentTimeMillis())) {
            putMemory(key, cache.value ?: "")
            return cache.value
        }
        return null
    }

    fun getInt(key: String): Int? {
        return get(key)?.toIntOrNull()
    }

    fun getLong(key: String): Long? {
        return get(key)?.toLongOrNull()
    }

    fun getDouble(key: String): Double? {
        return get(key)?.toDoubleOrNull()
    }

    fun getFloat(key: String): Float? {
        return get(key)?.toFloatOrNull()
    }

    fun getByteArray(key: String): ByteArray? {
        return ACache.get(appCtx).getAsBinary(key)
    }

    fun getQueryTTF(key: String): com.v2reading.reader.model.analyzeRule.QueryTTF? {
        val cache = queryTTFMap[key] ?: return null
        if (cache.first == 0L || cache.first > System.currentTimeMillis()) {
            return cache.second
        } else {
            queryTTFMap.remove(key)
        }
        return null
    }

    fun putFile(key: String, value: String, saveTime: Int = 0) {
        ACache.get(appCtx).put(key, value, saveTime)
    }

    fun getFile(key: String): String? {
        return ACache.get(appCtx).getAsString(key)
    }

    fun delete(key: String) {
        appDb.cacheDao.delete(key)
        deleteMemory(key)
        ACache.get(appCtx).remove(key)
    }
}