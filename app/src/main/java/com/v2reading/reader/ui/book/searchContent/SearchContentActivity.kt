package com.v2reading.reader.ui.book.searchContent

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import com.v2reading.reader.R
import com.v2reading.reader.base.VMBaseActivity
import com.v2reading.reader.constant.EventBus
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.Book
import com.v2reading.reader.data.entities.BookChapter
import com.v2reading.reader.databinding.ActivitySearchContentBinding
import com.v2reading.reader.help.BookHelp
import com.v2reading.reader.help.IntentData
import com.v2reading.reader.lib.theme.bottomBackground
import com.v2reading.reader.lib.theme.getPrimaryTextColor
import com.v2reading.reader.lib.theme.primaryTextColor
import com.v2reading.reader.ui.widget.recycler.UpLinearLayoutManager
import com.v2reading.reader.ui.widget.recycler.VerticalDivider
import com.v2reading.reader.utils.ColorUtils
import com.v2reading.reader.utils.applyTint
import com.v2reading.reader.utils.observeEvent
import com.v2reading.reader.utils.postEvent
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SearchContentActivity :
    VMBaseActivity<ActivitySearchContentBinding, SearchContentViewModel>(),
    SearchContentAdapter.Callback {

    override val binding by viewBinding(ActivitySearchContentBinding::inflate)
    override val viewModel by viewModels<SearchContentViewModel>()
    private val adapter by lazy { SearchContentAdapter(this, this) }
    private val mLayoutManager by lazy { UpLinearLayoutManager(this) }
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }
    private var durChapterIndex = 0

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        val bbg = bottomBackground
        val btc = getPrimaryTextColor(ColorUtils.isColorLight(bbg))
        binding.llSearchBaseInfo.setBackgroundColor(bbg)
        binding.tvCurrentSearchInfo.setTextColor(btc)
        binding.ivSearchContentTop.setColorFilter(btc)
        binding.ivSearchContentBottom.setColorFilter(btc)
        initSearchView()
        initRecyclerView()
        initView()
        intent.getStringExtra("bookUrl")?.let {
            viewModel.initBook(it) {
                initBook()
            }
        }
    }

    private fun initSearchView() {
        searchView.applyTint(primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.search)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String): Boolean {
                if (viewModel.lastQuery != query) {
                    startContentSearch(query)
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                return false
            }
        })
    }

    private fun initRecyclerView() {
        binding.recyclerView.layoutManager = mLayoutManager
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        binding.recyclerView.adapter = adapter
    }

    private fun initView() {
        binding.ivSearchContentTop.setOnClickListener {
            mLayoutManager.scrollToPositionWithOffset(0, 0)
        }
        binding.ivSearchContentBottom.setOnClickListener {
            if (adapter.itemCount > 0) {
                mLayoutManager.scrollToPositionWithOffset(adapter.itemCount - 1, 0)
            }
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initBook() {
        binding.tvCurrentSearchInfo.text =
            this.getString(R.string.search_content_size) + ": ${viewModel.searchResultCounts}"
        viewModel.book?.let {
            initCacheFileNames(it)
            durChapterIndex = it.durChapterIndex
            intent.getStringExtra("searchWord")?.let { searchWord ->
                searchView.setQuery(searchWord, true)
            }
        }
    }

    private fun initCacheFileNames(book: Book) {
        launch {
            withContext(IO) {
                viewModel.cacheChapterNames.addAll(BookHelp.getChapterFiles(book))
            }
            adapter.notifyItemRangeChanged(0, adapter.itemCount, true)
        }
    }

    override fun observeLiveBus() {
        observeEvent<BookChapter>(EventBus.SAVE_CONTENT) { chapter ->
            viewModel.book?.bookUrl?.let { bookUrl ->
                if (chapter.bookUrl == bookUrl) {
                    viewModel.cacheChapterNames.add(chapter.getFileName())
                    adapter.notifyItemChanged(chapter.index, true)
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    fun startContentSearch(query: String) {
        // 按章节搜索内容
        if (query.isNotBlank()) {
            adapter.clearItems()
            viewModel.searchResultList.clear()
            viewModel.searchResultCounts = 0
            viewModel.lastQuery = query
            launch {
                withContext(IO) {
                    appDb.bookChapterDao.getChapterList(viewModel.bookUrl)
                }.forEach { bookChapter ->
                    binding.refreshProgressBar.isAutoLoading = true
                    val searchResults = withContext(IO) {
                        if (isLocalBook || viewModel.cacheChapterNames.contains(bookChapter.getFileName())) {
                            viewModel.searchChapter(query, bookChapter)
                        } else {
                            null
                        }
                    }
                    binding.tvCurrentSearchInfo.text =
                        this@SearchContentActivity.getString(R.string.search_content_size) + ": ${viewModel.searchResultCounts}"
                    if (searchResults != null && searchResults.isNotEmpty()) {
                        viewModel.searchResultList.addAll(searchResults)
                        binding.refreshProgressBar.isAutoLoading = false
                        adapter.addItems(searchResults)
                    }
                }
                binding.refreshProgressBar.isAutoLoading = false
                if (viewModel.searchResultCounts == 0) {
                    val noSearchResult =
                        SearchResult(resultText = getString(R.string.search_content_empty))
                    adapter.addItem(noSearchResult)
                }
            }
        }
    }

    val isLocalBook: Boolean
        get() = viewModel.book?.isLocalBook() == true

    override fun openSearchResult(searchResult: SearchResult) {
        postEvent(EventBus.SEARCH_RESULT, viewModel.searchResultList as List<SearchResult>)
        val searchData = Intent()
        val key = System.currentTimeMillis()
        IntentData.put("searchResult$key", searchResult)
        IntentData.put("searchResultList$key", viewModel.searchResultList)
        searchData.putExtra("key", key)
        setResult(RESULT_OK, searchData)
        finish()
    }

    override fun durChapterIndex(): Int {
        return durChapterIndex
    }

}