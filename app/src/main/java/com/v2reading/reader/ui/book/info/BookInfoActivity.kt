package com.v2reading.reader.ui.book.info

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import com.tradplus.ads.base.bean.TPAdError
import com.tradplus.ads.base.bean.TPAdInfo
import com.tradplus.ads.base.bean.TPBaseAd
import com.tradplus.ads.open.nativead.NativeAdListener
import com.tradplus.ads.open.nativead.TPNative
import com.v2reading.reader.BuildConfig
import com.v2reading.reader.R
import com.v2reading.reader.base.VMBaseActivity
import com.v2reading.reader.constant.BookType
import com.v2reading.reader.constant.EventBus
import com.v2reading.reader.constant.Theme
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.Book
import com.v2reading.reader.data.entities.BookChapter
import com.v2reading.reader.data.entities.BookSource
import com.v2reading.reader.databinding.ActivityBookInfoBinding
import com.v2reading.reader.databinding.DialogEditTextBinding
import com.v2reading.reader.lib.dialogs.alert
import com.v2reading.reader.lib.theme.backgroundColor
import com.v2reading.reader.lib.theme.bottomBackground
import com.v2reading.reader.lib.theme.getPrimaryTextColor
import com.v2reading.reader.model.BookCover
import com.v2reading.reader.model.UpdateResponse
import com.v2reading.reader.ui.about.AppLogDialog
import com.v2reading.reader.ui.association.ImportOnLineBookFileDialog
import com.v2reading.reader.ui.book.audio.AudioPlayActivity
import com.v2reading.reader.ui.book.changecover.ChangeCoverDialog
import com.v2reading.reader.ui.book.changesource.ChangeBookSourceDialog
import com.v2reading.reader.ui.book.group.GroupSelectDialog
import com.v2reading.reader.ui.book.info.edit.BookInfoEditActivity
import com.v2reading.reader.ui.book.read.ReadBookActivity
import com.v2reading.reader.ui.book.remote.manager.RemoteBookWebDav
import com.v2reading.reader.ui.book.search.SearchActivity
import com.v2reading.reader.ui.book.source.edit.BookSourceEditActivity
import com.v2reading.reader.ui.book.toc.TocActivityResult
import com.v2reading.reader.ui.login.SourceLoginActivity
import com.v2reading.reader.ui.widget.dialog.PhotoDialog
import com.v2reading.reader.ui.widget.dialog.WaitDialog
import com.v2reading.reader.utils.*
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import constant.DownLoadBy
import constant.UiType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import listener.OnBtnClickListener
import model.UiConfig
import model.UpdateConfig
import org.apache.commons.lang3.RandomUtils
import update.UpdateAppUtils


class BookInfoActivity :
    VMBaseActivity<ActivityBookInfoBinding, BookInfoViewModel>(toolBarTheme = Theme.Dark),
    GroupSelectDialog.CallBack,
    ChangeBookSourceDialog.CallBack,
    ChangeCoverDialog.CallBack {

    private val tocActivityResult = registerForActivityResult(TocActivityResult()) {
        it?.let {
            viewModel.bookData.value?.let { book ->
                launch {
                    withContext(IO) {
                        book.durChapterIndex = it.first
                        book.durChapterPos = it.second
                        appDb.bookDao.update(book)
                    }
                    viewModel.chapterListData.value?.let { chapterList ->
                        binding.tvToc.text =
                            getString(R.string.toc_s, chapterList[book.durChapterIndex].title)
                    }
                    startReadActivity(book)
                }
            }
        } ?: let {
            if (!viewModel.inBookshelf) {
                viewModel.delBook()
            }
        }
    }
    private val readBookResult = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshData(intent)
        if (it.resultCode == RESULT_OK) {
            viewModel.inBookshelf = true
            upTvBookshelf()
            onAddBookSuccess()
        }
    }
    private val infoEditResult = registerForActivityResult(
        StartActivityContract(BookInfoEditActivity::class.java)
    ) {
        if (it.resultCode == RESULT_OK) {
            viewModel.upEditBook()
        }
    }
    private var tocChanged = false

    override val binding by viewBinding(ActivityBookInfoBinding::inflate)
    override val viewModel by viewModels<BookInfoViewModel>()

    val TAG = "BookInfo"

    private fun onAddBookSuccess() {



    }
    @SuppressLint("PrivateResource")
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.transparent()
        binding.arcView.setBgColor(backgroundColor)
        binding.llInfo.setBackgroundColor(backgroundColor)
        binding.scrollView.setBackgroundColor(backgroundColor)
        binding.flAction.setBackgroundColor(bottomBackground)
        binding.tvShelf.setTextColor(getPrimaryTextColor(ColorUtils.isColorLight(bottomBackground)))
        binding.tvToc.text = getString(R.string.toc_s, getString(R.string.loading))
        viewModel.bookData.observe(this) { showBook(it) }
        viewModel.chapterListData.observe(this) { upLoading(false, it) }
        viewModel.initData(intent)
        initViewEvent()

        if (RandomUtils.nextInt(0, 10) > 8) {
//            checkUpdate()
        }

        loadNormalNative()
    }

    private fun loadNormalNative() {
        val tpNative = TPNative(this, "5549C20BBC631241E209C3D8B8E97EFB")
        tpNative.setAdListener(object : NativeAdListener() {
            override fun onAdLoaded(tpAdInfo: TPAdInfo, tpBaseAd: TPBaseAd?) {
                Log.i(TAG, "onAdLoaded: " + tpAdInfo.adSourceName + "加载成功")
                tpNative.showAd(binding.adContainer, R.layout.tp_native_ad_list_item, "")
            }

            override fun onAdClicked(tpAdInfo: TPAdInfo) {
                Log.i(TAG, "onAdClicked: " + tpAdInfo.adSourceName + "被点击")
            }

            override fun onAdImpression(tpAdInfo: TPAdInfo) {
                Log.i(TAG, "onAdImpression: " + tpAdInfo.adSourceName + "展示")
            }

            override fun onAdShowFailed(tpAdError: TPAdError?, tpAdInfo: TPAdInfo) {
                Log.i(TAG, "onAdShowFailed: " + tpAdInfo.adSourceName + "展示失败")
            }

            override  fun onAdLoadFailed(tpAdError: TPAdError) {
                Log.i(
                    TAG,
                    "onAdLoadFailed: 加载失败 , code : " + tpAdError.getErrorCode() + ", msg :" + tpAdError.getErrorMsg()
                )
            }

            override fun onAdClosed(tpAdInfo: TPAdInfo) {
                Log.i(TAG, "onAdClosed: " + tpAdInfo.adSourceName + "广告关闭")
            }
        })
        tpNative.loadAd()
    }


    private fun checkUpdate() {

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = NetworkUtils.getRaw("reader/v2readpro2.json")
                val updateBean: UpdateResponse? =
                    GSON.fromJsonObject<UpdateResponse>(response).getOrNull()

                if ((updateBean?.versionCode ?: 0) > BuildConfig.VERSION_CODE) {

                    launch(Dispatchers.Main) {

                        UpdateAppUtils
                            .getInstance()
                            .apkUrl(updateBean?.url ?: "")
                            .updateTitle(updateBean?.updateTitle ?: "")
                            .updateContent(updateBean?.updateContent ?: "")
                            .updateConfig(UpdateConfig().apply {
                                downloadBy = DownLoadBy.APP
                                serverVersionName = updateBean?.versionName ?: ""
                                serverVersionCode = updateBean?.versionCode ?: 0
                                checkWifi = true
                                force =
                                    updateBean?.forceVersionCode!! > BuildConfig.VERSION_CODE
                            })
                            .uiConfig(
                                UiConfig(
                                    uiType = UiType.PLENTIFUL,
                                    updateLogoImgRes = R.mipmap.ic_launcher
                                )
                            )
                            // 设置 取消 按钮点击事件
                            .setCancelBtnClickListener(object : OnBtnClickListener {
                                override fun onClick(): Boolean {

                                    return false // 事件是否消费，是否需要传递下去。false-会执行原有点击逻辑，true-只执行本次设置的点击逻辑
                                }
                            })
                            // 设置 立即更新 按钮点击事件
                            .setUpdateBtnClickListener(object : OnBtnClickListener {
                                override fun onClick(): Boolean {

                                    return false // 事件是否消费，是否需要传递下去。false-会执行原有点击逻辑，true-只执行本次设置的点击逻辑
                                }
                            })
                            .update()
                    }

                } else {
//                    toastOnUi("已是最新版本")
                }

            } catch (e: Exception) {

            }
        }

    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_info, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_can_update)?.isChecked =
            viewModel.bookData.value?.canUpdate ?: true
        menu.findItem(R.id.menu_split_long_chapter)?.isChecked =
            viewModel.bookData.value?.getSplitLongChapter() ?: true
//        menu.findItem(R.id.menu_login)?.isVisible =
//            !viewModel.bookSource?.loginUrl.isNullOrBlank()
//        menu.findItem(R.id.menu_set_source_variable)?.isVisible =
//            viewModel.bookSource != null
//        menu.findItem(R.id.menu_set_book_variable)?.isVisible =
//            viewModel.bookSource != null
        menu.findItem(R.id.menu_can_update)?.isVisible =
            viewModel.bookSource != null
        menu.findItem(R.id.menu_split_long_chapter)?.isVisible =
            viewModel.bookData.value?.isLocalTxt() ?: false
        menu.findItem(R.id.menu_upload)?.isVisible =
            viewModel.bookData.value?.isLocalBook() ?: false
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_edit -> {
                if (viewModel.inBookshelf) {
                    viewModel.bookData.value?.let {
                        infoEditResult.launch {
                            putExtra("bookUrl", it.bookUrl)
                        }
                    }
                } else {
                    toastOnUi(R.string.after_add_bookshelf)
                }
            }
            R.id.menu_refresh -> {
                upLoading(true)
                viewModel.bookData.value?.let {
                    if (it.isLocalBook()) {
                        it.tocUrl = ""
                    }
                    viewModel.loadBookInfo(it, false)
                }
            }
            R.id.menu_login -> viewModel.bookSource?.let {
                startActivity<SourceLoginActivity> {
                    putExtra("type", "bookSource")
                    putExtra("key", it.bookSourceUrl)
                }
            }
            R.id.menu_top -> viewModel.topBook()
            R.id.menu_set_source_variable -> setSourceVariable()
            R.id.menu_set_book_variable -> setBookVariable()
            R.id.menu_copy_book_url -> viewModel.bookData.value?.bookUrl?.let {
                sendToClip(it)
            } ?: toastOnUi(R.string.no_book)
            R.id.menu_copy_toc_url -> viewModel.bookData.value?.tocUrl?.let {
                sendToClip(it)
            } ?: toastOnUi(R.string.no_book)
            R.id.menu_can_update -> {
                if (viewModel.inBookshelf) {
                    viewModel.bookData.value?.let {
                        it.canUpdate = !it.canUpdate
                        viewModel.saveBook(it)
                    }
                } else {
                    toastOnUi(R.string.after_add_bookshelf)
                }
            }
            R.id.menu_clear_cache -> viewModel.clearCache()
            R.id.menu_log -> showDialogFragment<AppLogDialog>()
            R.id.menu_split_long_chapter -> {
                upLoading(true)
                tocChanged = true
                viewModel.bookData.value?.let {
                    it.setSplitLongChapter(!item.isChecked)
                    viewModel.loadBookInfo(it, false)
                }
                item.isChecked = !item.isChecked
                if (!item.isChecked) longToastOnUi(R.string.need_more_time_load_content)
            }

            R.id.menu_upload -> {
                launch {
                    val uri = Uri.parse(viewModel.bookData.value?.bookUrl.toString())
                    val waitDialog = WaitDialog(this@BookInfoActivity)
                    waitDialog.setText("上传中.....")
                    waitDialog.show()
                    try {
                        val isUpload = RemoteBookWebDav.upload(uri)
                        if (isUpload)
                            toastOnUi(getString(R.string.upload_book_success))
                        else
                            toastOnUi(getString(R.string.upload_book_fail))
                    } catch (e: Exception) {
                        toastOnUi(e.localizedMessage)
                    } finally {
                        waitDialog.dismiss()
                    }
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun showBook(book: Book) = binding.run {
        showCover(book)
        tvName.text = book.name
        tvAuthor.text = getString(R.string.author_show, book.getRealAuthor())
        tvOrigin.text = getString(R.string.origin_show, book.originName)
        tvLasted.text = getString(R.string.lasted_show, book.latestChapterTitle)
        tvIntro.text = book.getDisplayIntro()
        upTvBookshelf()
        val kinds = book.getKindList()
        if (kinds.isEmpty()) {
            lbKind.gone()
        } else {
            lbKind.visible()
            lbKind.setLabels(kinds)
        }
        upGroup(book.group)
    }

    private fun showCover(book: Book) {
        binding.ivCover.load(book.getDisplayCover(), book.name, book.author, false, book.origin)
        BookCover.loadBlur(this, book.getDisplayCover())
            .into(binding.bgBook)
    }

    private fun upLoading(isLoading: Boolean, chapterList: List<BookChapter>? = null) {
        when {
            isLoading -> {
                binding.tvToc.text = getString(R.string.toc_s, getString(R.string.loading))
            }
            chapterList.isNullOrEmpty() -> {
                binding.tvToc.text =
                    if (viewModel.isImportBookOnLine) getString(R.string.click_read_button_load) else getString(
                        R.string.toc_s,
                        getString(R.string.error_load_toc)
                    )
            }
            else -> {
                viewModel.bookData.value?.let {
                    if (it.durChapterIndex < chapterList.size) {
                        binding.tvToc.text =
                            getString(R.string.toc_s, chapterList[it.durChapterIndex].title)
                    } else {
                        binding.tvToc.text = getString(R.string.toc_s, chapterList.last().title)
                    }
                }
            }
        }
    }

    private fun upTvBookshelf() {
        if (viewModel.inBookshelf) {
            binding.tvShelf.text = getString(R.string.remove_from_bookshelf)
        } else {
            binding.tvShelf.text = getString(R.string.add_to_shelf)
        }
    }

    private fun upGroup(groupId: Long) {
        viewModel.loadGroup(groupId) {
            if (it.isNullOrEmpty()) {
                binding.tvGroup.text = getString(R.string.group_s, getString(R.string.no_group))
            } else {
                binding.tvGroup.text = getString(R.string.group_s, it)
            }
        }
    }

    private fun initViewEvent() = binding.run {
        ivCover.setOnClickListener {
            viewModel.bookData.value?.let {
                showDialogFragment(
                    ChangeCoverDialog(it.name, it.author)
                )
            } ?: toastOnUi("Book is null")
        }
        ivCover.setOnLongClickListener {
            viewModel.bookData.value?.getDisplayCover()?.let { path ->
                showDialogFragment(PhotoDialog(path))
            }
            true
        }
        tvRead.setOnClickListener {
            viewModel.bookData.value?.let { book ->
                if (viewModel.isImportBookOnLine) {
                    showDialogFragment<ImportOnLineBookFileDialog> {
                        putString("bookUrl", book.bookUrl)
                    }
                } else {
                    readBook(book)
                }
            } ?: toastOnUi("Book is null")
        }
        tvShelf.setOnClickListener {
            if (viewModel.inBookshelf) {
                deleteBook()
            } else {
                viewModel.addToBookshelf {
                    upTvBookshelf()
                    onAddBookSuccess()
                }
            }
        }
        tvOrigin.setOnClickListener {
            viewModel.bookData.value?.let {
                startActivity<BookSourceEditActivity> {
                    putExtra("sourceUrl", it.origin)
                }
            } ?: toastOnUi("Book is null")
        }
        tvChangeSource.setOnClickListener {
            viewModel.bookData.value?.let { book ->
                showDialogFragment(ChangeBookSourceDialog(book.name, book.author))
            } ?: toastOnUi("Book is null")
        }
        tvTocView.setOnClickListener {
            if (!viewModel.inBookshelf) {
                viewModel.saveBook(viewModel.bookData.value) {
                    viewModel.saveChapterList {
                        openChapterList()
                    }
                }
            } else {
                openChapterList()
            }
        }
        tvChangeGroup.setOnClickListener {
            viewModel.bookData.value?.let {
                showDialogFragment(
                    GroupSelectDialog(it.group)
                )
            } ?: toastOnUi("Book is null")
        }
        tvAuthor.setOnClickListener {
            startActivity<SearchActivity> {
                putExtra("key", viewModel.bookData.value?.author)
            }
        }
        tvName.setOnClickListener {
            startActivity<SearchActivity> {
                putExtra("key", viewModel.bookData.value?.name)
            }
        }
    }

    private fun setSourceVariable() {
        launch {
            val source = viewModel.bookSource
            if (source == null) {
                toastOnUi("站点不存在")
                return@launch
            }
            val variable = withContext(IO) { source.getVariable() }
            alert(R.string.set_source_variable) {
                setMessage(source.getDisplayVariableComment("源变量可在js中通过source.getVariable()获取"))
                val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                    editView.hint = "source variable"
                    editView.setText(variable)
                }
                customView { alertBinding.root }
                okButton {
                    viewModel.bookSource?.setVariable(alertBinding.editView.text?.toString())
                }
                cancelButton()
                neutralButton(R.string.delete) {
                    viewModel.bookSource?.setVariable(null)
                }
            }
        }
    }

    private fun setBookVariable() {
        launch {
            val source = viewModel.bookSource
            if (source == null) {
                toastOnUi("站点不存在")
                return@launch
            }
            val variable = withContext(IO) { viewModel.bookData.value?.getVariable("custom") }
            alert(R.string.set_source_variable) {
                setMessage(source.getDisplayVariableComment("""书籍变量可在js中通过book.getVariable("custom")获取"""))
                val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                    editView.hint = "book variable"
                    editView.setText(variable)
                }
                customView { alertBinding.root }
                okButton {
                    viewModel.bookData.value?.let { book ->
                        book.putVariable("custom", alertBinding.editView.text?.toString())
                        viewModel.saveBook(book)
                    }
                }
                cancelButton()
                neutralButton(R.string.delete) {
                    viewModel.bookData.value?.let { book ->
                        book.putVariable("custom", null)
                        viewModel.saveBook(book)
                    }
                }
            }
        }
    }

    @SuppressLint("InflateParams")
    private fun deleteBook() {
        viewModel.bookData.value?.let {
            if (it.isLocalBook()) {
                alert(
                    titleResource = R.string.sure,
                    messageResource = R.string.sure_del
                ) {
                    val checkBox = CheckBox(this@BookInfoActivity).apply {
                        setText(R.string.delete_book_file)
                    }
                    val view = LinearLayout(this@BookInfoActivity).apply {
                        setPadding(16.dpToPx(), 0, 16.dpToPx(), 0)
                        addView(checkBox)
                    }
                    customView { view }
                    positiveButton(R.string.yes) {
                        viewModel.delBook(checkBox.isChecked) {
                            finish()
                        }
                    }
                    negativeButton(R.string.no)
                }
            } else {
                viewModel.delBook {
                    upTvBookshelf()
                }
            }
        }
    }

    private fun openChapterList() {
        if (viewModel.chapterListData.value.isNullOrEmpty()) {
            toastOnUi(R.string.chapter_list_empty)
            return
        }
        viewModel.bookData.value?.let {
            tocActivityResult.launch(it.bookUrl)
        }
    }

    private fun readBook(book: Book) {
        if (!viewModel.inBookshelf) {
            viewModel.saveBook(book) {
                viewModel.saveChapterList {
                    startReadActivity(book)
                }
            }
        } else {
            viewModel.saveBook(book) {
                startReadActivity(book)
            }
        }
    }

    private fun startReadActivity(book: Book) {
        when (book.type) {
            BookType.audio -> readBookResult.launch(
                Intent(this, AudioPlayActivity::class.java)
                    .putExtra("bookUrl", book.bookUrl)
                    .putExtra("inBookshelf", viewModel.inBookshelf)
            )
            else -> readBookResult.launch(
                Intent(this, ReadBookActivity::class.java)
                    .putExtra("bookUrl", book.bookUrl)
                    .putExtra("inBookshelf", viewModel.inBookshelf)
                    .putExtra("tocChanged", tocChanged)
            )
        }
        tocChanged = false
    }

    override val oldBook: Book?
        get() = viewModel.bookData.value

    override fun changeTo(source: BookSource, book: Book, toc: List<BookChapter>) {
        viewModel.changeTo(source, book, toc)
    }

    override fun coverChangeTo(coverUrl: String) {
        viewModel.bookData.value?.let { book ->
            book.customCoverUrl = coverUrl
            viewModel.saveBook(book)
            showCover(book)
        }
    }

    override fun upGroup(requestCode: Int, groupId: Long) {
        upGroup(groupId)
        viewModel.bookData.value?.let { book ->
            book.group = groupId
            if (viewModel.inBookshelf) {
                viewModel.saveBook(book)
            } else if (groupId > 0) {
                viewModel.saveBook(book)
                viewModel.inBookshelf = true
                upTvBookshelf()
            }
        }
    }

    override fun observeLiveBus() {
        observeEvent<String>(EventBus.BOOK_URL_CHANGED) {
            viewModel.changeToLocalBook(it)
        }
    }
}