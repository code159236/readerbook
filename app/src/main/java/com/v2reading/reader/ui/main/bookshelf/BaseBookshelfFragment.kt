package com.v2reading.reader.ui.main.bookshelf

import android.annotation.SuppressLint
import android.view.Menu
import android.view.MenuItem
import androidx.fragment.app.activityViewModels
import androidx.fragment.app.viewModels
import androidx.lifecycle.LiveData
import com.v2reading.reader.R
import com.v2reading.reader.base.VMBaseFragment
import com.v2reading.reader.constant.EventBus
import com.v2reading.reader.constant.PreferKey
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.Book
import com.v2reading.reader.data.entities.BookGroup
import com.v2reading.reader.databinding.DialogBookshelfConfigBinding
import com.v2reading.reader.databinding.DialogEditTextBinding
import com.v2reading.reader.help.DirectLinkUpload
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.lib.dialogs.alert
import com.v2reading.reader.ui.about.AppLogDialog
import com.v2reading.reader.ui.book.cache.CacheActivity
import com.v2reading.reader.ui.book.group.GroupManageDialog
import com.v2reading.reader.ui.book.local.ImportBookActivity
import com.v2reading.reader.ui.book.manage.BookshelfManageActivity
import com.v2reading.reader.ui.book.remote.RemoteBookActivity
import com.v2reading.reader.ui.book.search.SearchActivity
import com.v2reading.reader.ui.document.HandleFileContract
import com.v2reading.reader.ui.main.MainViewModel
import com.v2reading.reader.utils.*

abstract class BaseBookshelfFragment(layoutId: Int) : VMBaseFragment<BookshelfViewModel>(layoutId) {

    val activityViewModel by activityViewModels<MainViewModel>()
    override val viewModel by viewModels<BookshelfViewModel>()

    private val importBookshelf = registerForActivityResult(HandleFileContract()) {
        kotlin.runCatching {
            it.uri?.readText(requireContext())?.let { text ->
                viewModel.importBookshelf(text, groupId)
            }
        }.onFailure {
            toastOnUi(it.localizedMessage ?: "ERROR")
        }
    }
    private val exportResult = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            alert(R.string.export_success) {
                if (uri.toString().isAbsUrl()) {
                    DirectLinkUpload.getSummary()?.let { summary ->
                        setMessage(summary)
                    }
                }
                val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                    editView.hint = getString(R.string.path)
                    editView.setText(uri.toString())
                }
                customView { alertBinding.root }
                okButton {
                    requireContext().sendToClip(uri.toString())
                }
            }
        }
    }
    abstract val groupId: Long
    abstract val books: List<Book>
    private var groupsLiveData: LiveData<List<BookGroup>>? = null

    abstract fun gotoTop()

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        menuInflater.inflate(R.menu.main_bookshelf, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        when (item.itemId) {
            // 查看远程书籍
            R.id.menu_remote -> startActivity<RemoteBookActivity>()
            R.id.menu_search -> startActivity<SearchActivity>()
            R.id.menu_update_toc -> activityViewModel.upToc(books)
            R.id.menu_bookshelf_layout -> configBookshelf()
            R.id.menu_group_manage -> showDialogFragment<GroupManageDialog>()
            R.id.menu_add_local -> startActivity<ImportBookActivity>()
            R.id.menu_add_url -> addBookByUrl()
            R.id.menu_bookshelf_manage -> startActivity<BookshelfManageActivity> {
                putExtra("groupId", groupId)
            }
            R.id.menu_download -> startActivity<CacheActivity> {
                putExtra("groupId", groupId)
            }
            R.id.menu_export_bookshelf -> viewModel.exportBookshelf(books) { file ->
                exportResult.launch {
                    mode = HandleFileContract.EXPORT
                    fileData = Triple("bookshelf.json", file, "application/json")
                }
            }
            R.id.menu_import_bookshelf -> importBookshelfAlert(groupId)
            R.id.menu_log -> showDialogFragment<AppLogDialog>()
        }
    }

    protected fun initBookGroupData() {
        groupsLiveData?.removeObservers(viewLifecycleOwner)
        groupsLiveData = appDb.bookGroupDao.show.apply {
            observe(viewLifecycleOwner) {
                upGroup(it)
            }
        }
    }

    abstract fun upGroup(data: List<BookGroup>)

    @SuppressLint("InflateParams")
    fun addBookByUrl() {
        alert(titleResource = R.string.add_book_url) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url"
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    viewModel.addBookByUrl(it)
                }
            }
            noButton()
        }
    }

    @SuppressLint("InflateParams")
    fun configBookshelf() {
        alert(titleResource = R.string.bookshelf_layout) {
            val bookshelfLayout = getPrefInt(PreferKey.bookshelfLayout)
            val bookshelfSort = getPrefInt(PreferKey.bookshelfSort)
            val alertBinding =
                DialogBookshelfConfigBinding.inflate(layoutInflater)
                    .apply {
                        spGroupStyle.setSelection(AppConfig.bookGroupStyle)
                        swShowUnread.isChecked = AppConfig.showUnread
                        rgLayout.checkByIndex(bookshelfLayout)
                        rgSort.checkByIndex(bookshelfSort)
                    }
            customView { alertBinding.root }
            okButton {
                alertBinding.apply {
                    if (AppConfig.bookGroupStyle != spGroupStyle.selectedItemPosition) {
                        AppConfig.bookGroupStyle = spGroupStyle.selectedItemPosition
                        postEvent(EventBus.NOTIFY_MAIN, false)
                    }
                    if (AppConfig.showUnread != swShowUnread.isChecked) {
                        AppConfig.showUnread = swShowUnread.isChecked
                        postEvent(EventBus.BOOKSHELF_REFRESH, "")
                    }
                    var changed = false
                    if (bookshelfLayout != rgLayout.getCheckedIndex()) {
                        putPrefInt(PreferKey.bookshelfLayout, rgLayout.getCheckedIndex())
                        changed = true
                    }
                    if (bookshelfSort != rgSort.getCheckedIndex()) {
                        putPrefInt(PreferKey.bookshelfSort, rgSort.getCheckedIndex())
                        changed = true
                    }
                    if (changed) {
                        postEvent(EventBus.RECREATE, "")
                    }
                }
            }
            cancelButton()
        }
    }


    private fun importBookshelfAlert(groupId: Long) {
        alert(titleResource = R.string.import_bookshelf) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url/json"
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.editView.text?.toString()?.let {
                    viewModel.importBookshelf(it, groupId)
                }
            }
            noButton()
            neutralButton(R.string.select_file) {
                importBookshelf.launch {
                    mode = HandleFileContract.FILE
                    allowExtensions = arrayOf("txt", "json")
                }
            }
        }
    }

}