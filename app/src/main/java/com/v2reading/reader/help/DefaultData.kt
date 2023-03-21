package com.v2reading.reader.help

import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.HttpTTS
import com.v2reading.reader.data.entities.KeyboardAssist
import com.v2reading.reader.data.entities.TxtTocRule
import com.v2reading.reader.help.config.ReadBookConfig
import com.v2reading.reader.help.config.ThemeConfig
import com.v2reading.reader.model.BookCover
import com.v2reading.reader.utils.GSON
import com.v2reading.reader.utils.NetworkUtils
import com.v2reading.reader.utils.fromJsonArray
import com.v2reading.reader.utils.fromJsonObject
import kotlinx.coroutines.runBlocking
import splitties.init.appCtx
import java.io.File

object DefaultData {

    val httpTTS: List<HttpTTS> by lazy {
        runBlocking {
            val json = NetworkUtils.getRaw("v2reading/tts.json")
            HttpTTS.fromJsonArray(json).getOrElse {
                emptyList()
            }
        }
    }

    val readConfigs: List<ReadBookConfig.Config> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}${ReadBookConfig.configFileName}")
                .readBytes()
        )
        GSON.fromJsonArray<ReadBookConfig.Config>(json).getOrNull()
            ?: emptyList()
    }

    val txtTocRules: List<TxtTocRule> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}txtTocRule.json")
                .readBytes()
        )
        GSON.fromJsonArray<TxtTocRule>(json).getOrNull() ?: emptyList()
    }

    val themeConfigs: List<ThemeConfig.Config> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}${ThemeConfig.configFileName}")
                .readBytes()
        )
        GSON.fromJsonArray<ThemeConfig.Config>(json).getOrNull() ?: emptyList()
    }

//    val rssSources: List<RssSource> by lazy {
//        val json = String(
//            appCtx.assets.open("defaultData${File.separator}rssSources.json")
//                .readBytes()
//        )
//        RssSource.fromJsonArray(json).getOrDefault(emptyList())
//    }
//    val bookSources: List<BookSource> by lazy {
//        val json = String(
//            appCtx.assets.open("defaultData${File.separator}bookSources.json")
//                .readBytes()
//        )
//        BookSource.fromJsonArray(json).getOrDefault(emptyList())
//    }

    val coverRuleConfig: BookCover.CoverRuleConfig by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}coverRuleConfig.json")
                .readBytes()
        )
        GSON.fromJsonObject<BookCover.CoverRuleConfig>(json).getOrThrow()!!
    }

    val keyboardAssists: List<KeyboardAssist> by lazy {
        val json = String(
            appCtx.assets.open("defaultData${File.separator}keyboardAssists.json")
                .readBytes()
        )
        GSON.fromJsonArray<KeyboardAssist>(json).getOrNull()!!
    }

    fun importDefaultHttpTTS() {
        appDb.httpTTSDao.deleteDefault()
        appDb.httpTTSDao.insert(*httpTTS.toTypedArray())
    }

    fun importDefaultTocRules() {
        appDb.txtTocRuleDao.deleteDefault()
        appDb.txtTocRuleDao.insert(*txtTocRules.toTypedArray())
    }

    fun importDefaultRssSources() {
//        appDb.bookSourceDao.insert(*bookSources.toTypedArray())
//        appDb.rssSourceDao.insert(*rssSources.toTypedArray())
    }

}