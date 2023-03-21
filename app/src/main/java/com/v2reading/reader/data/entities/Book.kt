package com.v2reading.reader.data.entities

import android.os.Parcelable
import androidx.room.*
import com.v2reading.reader.constant.AppPattern
import com.v2reading.reader.constant.BookType
import com.v2reading.reader.constant.PageAnim
import com.v2reading.reader.data.appDb
import com.v2reading.reader.help.BookHelp
import com.v2reading.reader.help.ContentProcessor
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.help.config.ReadBookConfig
import com.v2reading.reader.model.ReadBook
import com.v2reading.reader.utils.GSON
import com.v2reading.reader.utils.MD5Utils
import com.v2reading.reader.utils.fromJsonObject
import kotlinx.coroutines.runBlocking
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import java.nio.charset.Charset
import kotlin.math.max
import kotlin.math.min

@Parcelize
@TypeConverters(Book.Converters::class)
@Entity(
    tableName = "books",
    indices = [Index(value = ["name", "author"], unique = true)]
)
data class Book(
    // 详情页Url(本地站点存储完整文件路径)
    @PrimaryKey
    @ColumnInfo(defaultValue = "")
    override var bookUrl: String = "",
    // 目录页Url (toc=table of Contents)
    @ColumnInfo(defaultValue = "")
    var tocUrl: String = "",
    // 站点URL(默认BookType.local)
    @ColumnInfo(defaultValue = "")
    var origin: String = BookType.local,
    //站点名称 or 本地书籍文件名
    @ColumnInfo(defaultValue = "")
    var originName: String = "",
    // 书籍名称(站点获取)
    @ColumnInfo(defaultValue = "")
    override var name: String = "",
    // 作者名称(站点获取)
    @ColumnInfo(defaultValue = "")
    override var author: String = "",
    // 分类信息(站点获取)
    override var kind: String? = null,
    // 分类信息(用户修改)
    var customTag: String? = null,
    // 封面Url(站点获取)
    var coverUrl: String? = null,
    // 封面Url(用户修改)
    var customCoverUrl: String? = null,
    // 简介内容(站点获取)
    var intro: String? = null,
    // 简介内容(用户修改)
    var customIntro: String? = null,
    // 自定义字符集名称(仅适用于本地书籍)
    var charset: String? = null,
    // 0:text 1:audio 3:image
    @ColumnInfo(defaultValue = "0")
    var type: Int = 0,
    // 自定义分组索引号
    @ColumnInfo(defaultValue = "0")
    var group: Long = 0,
    // 最新章节标题
    var latestChapterTitle: String? = null,
    // 最新章节标题更新时间
    @ColumnInfo(defaultValue = "0")
    var latestChapterTime: Long = System.currentTimeMillis(),
    // 最近一次更新书籍信息的时间
    @ColumnInfo(defaultValue = "0")
    var lastCheckTime: Long = System.currentTimeMillis(),
    // 最近一次站点新章节的数量
    @ColumnInfo(defaultValue = "0")
    var lastCheckCount: Int = 0,
    // 书籍目录总数
    @ColumnInfo(defaultValue = "0")
    var totalChapterNum: Int = 0,
    // 当前章节名称
    var durChapterTitle: String? = null,
    // 当前章节索引
    @ColumnInfo(defaultValue = "0")
    var durChapterIndex: Int = 0,
    // 当前阅读的进度(首行字符的索引位置)
    @ColumnInfo(defaultValue = "0")
    var durChapterPos: Int = 0,
    // 最近一次阅读书籍的时间(打开正文的时间)
    @ColumnInfo(defaultValue = "0")
    var durChapterTime: Long = System.currentTimeMillis(),
    override var wordCount: String? = null,
    // 刷新书架时更新书籍信息
    @ColumnInfo(defaultValue = "1")
    var canUpdate: Boolean = true,
    // 手动排序
    @ColumnInfo(defaultValue = "0")
    var order: Int = 0,
    //站点排序
    @ColumnInfo(defaultValue = "0")
    var originOrder: Int = 0,
    // 自定义书籍变量信息(用于站点规则检索书籍信息)
    override var variable: String? = null,
    var readConfig: ReadConfig? = null
) : Parcelable, BaseBook {

    fun isLocalBook(): Boolean {
        return origin == BookType.local
    }

    fun isLocalTxt(): Boolean {
        return isLocalBook() && originName.endsWith(".txt", true)
    }

    fun isEpub(): Boolean {
        return originName.endsWith(".epub", true)
    }

    fun isUmd(): Boolean {
        return originName.endsWith(".umd", true)
    }

    @Suppress("unused")
    fun isOnLineTxt(): Boolean {
        return !isLocalBook() && type == 0
    }

    override fun equals(other: Any?): Boolean {
        if (other is Book) {
            return other.bookUrl == bookUrl
        }
        return false
    }

    override fun hashCode(): Int {
        return bookUrl.hashCode()
    }

    @delegate:Transient
    @delegate:Ignore
    @IgnoredOnParcel
    override val variableMap: HashMap<String, String> by lazy {
        GSON.fromJsonObject<HashMap<String, String>>(variable).getOrNull() ?: hashMapOf()
    }

    @Ignore
    @IgnoredOnParcel
    override var infoHtml: String? = null

    @Ignore
    @IgnoredOnParcel
    override var tocHtml: String? = null

    @Ignore
    @IgnoredOnParcel
    var downloadUrls: List<String>? = null

    fun getRealAuthor() = author.replace(AppPattern.authorRegex, "")

    fun getUnreadChapterNum() = max(totalChapterNum - durChapterIndex - 1, 0)

    fun getDisplayCover() = if (customCoverUrl.isNullOrEmpty()) coverUrl else customCoverUrl

    fun getDisplayIntro() = if (customIntro.isNullOrEmpty()) intro else customIntro

    //自定义简介有自动更新的需求时，可通过更新intro再调用upCustomIntro()完成
    @Suppress("unused")
    fun upCustomIntro() {
        customIntro = intro
    }

    fun fileCharset(): Charset {
        return charset(charset ?: "UTF-8")
    }

    @IgnoredOnParcel
    val config: ReadConfig
        get() {
            if (readConfig == null) {
                readConfig = ReadConfig()
            }
            return readConfig!!
        }

    fun setReverseToc(reverseToc: Boolean) {
        config.reverseToc = reverseToc
    }

    fun getReverseToc(): Boolean {
        return config.reverseToc
    }

    fun setUseReplaceRule(useReplaceRule: Boolean) {
        config.useReplaceRule = useReplaceRule
    }

    fun getUseReplaceRule(): Boolean {
        val useReplaceRule = config.useReplaceRule
        if (useReplaceRule != null) {
            return useReplaceRule
        }
        //图片类站点 epub本地 默认关闭净化
        if (type == BookType.image || isEpub()) {
            return false
        }
        return AppConfig.replaceEnableDefault
    }

    fun setReSegment(reSegment: Boolean) {
        config.reSegment = reSegment
    }

    fun getReSegment(): Boolean {
        return config.reSegment
    }

    fun setPageAnim(pageAnim: Int?) {
        config.pageAnim = pageAnim
    }

    fun getPageAnim(): Int {
        var pageAnim = config.pageAnim
            ?: if (type == BookType.image) PageAnim.scrollPageAnim else ReadBookConfig.pageAnim
        if (pageAnim < 0) {
            pageAnim = ReadBookConfig.pageAnim
        }
        return pageAnim
    }

    fun setImageStyle(imageStyle: String?) {
        config.imageStyle = imageStyle
    }

    fun getImageStyle(): String? {
        return config.imageStyle
            ?: if (type == BookType.image) imgStyleFull else null
    }

    fun setTtsEngine(ttsEngine: String?) {
        config.ttsEngine = ttsEngine
    }

    fun getTtsEngine(): String? {
        return config.ttsEngine
    }

    fun setSplitLongChapter(limitLongContent: Boolean) {
        config.splitLongChapter = limitLongContent
    }

    fun getSplitLongChapter(): Boolean {
        return config.splitLongChapter
    }

    fun getDelTag(tag: Long): Boolean {
        return config.delTag and tag == tag
    }

    fun getFolderName(): String {
        //防止书名过长,只取9位
        var folderName = name.replace(AppPattern.fileNameRegex, "")
        folderName = folderName.substring(0, min(9, folderName.length))
        return folderName + MD5Utils.md5Encode16(bookUrl)
    }

    fun toSearchBook() = SearchBook(
        name = name,
        author = author,
        kind = kind,
        bookUrl = bookUrl,
        origin = origin,
        originName = originName,
        type = type,
        wordCount = wordCount,
        latestChapterTitle = latestChapterTitle,
        coverUrl = coverUrl,
        intro = intro,
        tocUrl = tocUrl,
        originOrder = originOrder,
        variable = variable
    ).apply {
        this.infoHtml = this@Book.infoHtml
        this.tocHtml = this@Book.tocHtml
    }

    fun changeTo(newBook: Book, toc: List<BookChapter>): Book {
        newBook.durChapterIndex = BookHelp
            .getDurChapter(durChapterIndex, durChapterTitle, toc, totalChapterNum)
        newBook.durChapterTitle = runBlocking {
            toc[newBook.durChapterIndex].getDisplayTitle(
                ContentProcessor.get(newBook.name, newBook.origin).getTitleReplaceRules()
            )
        }
        newBook.durChapterPos = durChapterPos
        newBook.group = group
        newBook.order = order
        newBook.customCoverUrl = customCoverUrl
        newBook.customIntro = customIntro
        newBook.customTag = customTag
        newBook.canUpdate = canUpdate
        newBook.readConfig = readConfig
        return newBook
    }

    fun createBookMark(): Bookmark {
        return Bookmark(
            bookName = name,
            bookAuthor = author,
        )
    }

    fun save() {
        if (appDb.bookDao.has(bookUrl) == true) {
            appDb.bookDao.update(this)
        } else {
            appDb.bookDao.insert(this)
        }
    }

    fun delete() {
        if (ReadBook.book?.bookUrl == bookUrl) {
            ReadBook.book = null
        }
        appDb.bookDao.delete(this)
    }

    companion object {
        const val hTag = 2L
        const val rubyTag = 4L
        const val imgStyleDefault = "DEFAULT"
        const val imgStyleFull = "FULL"
        const val imgStyleText = "TEXT"
    }

    @Parcelize
    data class ReadConfig(
        var reverseToc: Boolean = false,
        var pageAnim: Int? = null,
        var reSegment: Boolean = false,
        var imageStyle: String? = null,
        var useReplaceRule: Boolean? = null,// 正文使用净化替换规则
        var delTag: Long = 0L,//去除标签
        var ttsEngine: String? = null,
        var splitLongChapter: Boolean = true
    ) : Parcelable

    class Converters {

        @TypeConverter
        fun readConfigToString(config: ReadConfig?): String = GSON.toJson(config)

        @TypeConverter
        fun stringToReadConfig(json: String?) = GSON.fromJsonObject<ReadConfig>(json).getOrNull()
    }
}
