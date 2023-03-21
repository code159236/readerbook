package com.v2reading.reader.ui.book.toc

import android.annotation.SuppressLint
import android.app.Activity.RESULT_OK
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.v2reading.reader.R
import com.v2reading.reader.base.VMBaseFragment
import com.v2reading.reader.constant.EventBus
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.Book
import com.v2reading.reader.data.entities.BookChapter
import com.v2reading.reader.databinding.FragmentChapterListBinding
import com.v2reading.reader.help.BookHelp
import com.v2reading.reader.lib.theme.bottomBackground
import com.v2reading.reader.lib.theme.getPrimaryTextColor
import com.v2reading.reader.ui.widget.recycler.UpLinearLayoutManager
import com.v2reading.reader.ui.widget.recycler.VerticalDivider
import com.v2reading.reader.utils.ColorUtils
import com.v2reading.reader.utils.observeEvent
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers.Default
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChapterListFragment : VMBaseFragment<TocViewModel>(R.layout.fragment_chapter_list),
    ChapterListAdapter.Callback,
    TocViewModel.ChapterListCallBack {
    override val viewModel by activityViewModels<TocViewModel>()
    private val binding by viewBinding(FragmentChapterListBinding::bind)
    private val mLayoutManager by lazy { UpLinearLayoutManager(requireContext()) }
    private val adapter by lazy { ChapterListAdapter(requireContext(), this) }
    private var durChapterIndex = 0

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) = binding.run {
        viewModel.chapterListCallBack = this@ChapterListFragment
        val bbg = bottomBackground
        val btc = requireContext().getPrimaryTextColor(ColorUtils.isColorLight(bbg))
        llChapterBaseInfo.setBackgroundColor(bbg)
        tvCurrentChapterInfo.setTextColor(btc)
        ivChapterTop.setColorFilter(btc)
        ivChapterBottom.setColorFilter(btc)
        initRecyclerView()
        initView()
        viewModel.bookData.observe(this@ChapterListFragment) {
            initBook(it)
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.layoutManager = mLayoutManager
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = adapter
    }

    private fun initView() = binding.run {
        ivChapterTop.setOnClickListener {
            mLayoutManager.scrollToPositionWithOffset(0, 0)
        }
        ivChapterBottom.setOnClickListener {
            if (adapter.itemCount > 0) {
                mLayoutManager.scrollToPositionWithOffset(adapter.itemCount - 1, 0)
            }
        }
        tvCurrentChapterInfo.setOnClickListener {
            mLayoutManager.scrollToPositionWithOffset(durChapterIndex, 0)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun initBook(book: Book) {
        launch {
            upChapterList(null)
            durChapterIndex = book.durChapterIndex
            binding.tvCurrentChapterInfo.text =
                "${book.durChapterTitle}(${book.durChapterIndex + 1}/${book.totalChapterNum})"
            initCacheFileNames(book)
        }
    }

    private fun initCacheFileNames(book: Book) {
        launch(IO) {
            adapter.cacheFileNames.addAll(BookHelp.getChapterFiles(book))
            withContext(Main) {
                adapter.notifyItemRangeChanged(0, adapter.itemCount, true)
            }
        }
    }

    override fun observeLiveBus() {
        observeEvent<BookChapter>(EventBus.SAVE_CONTENT) { chapter ->
            viewModel.bookData.value?.bookUrl?.let { bookUrl ->
                if (chapter.bookUrl == bookUrl) {
                    adapter.cacheFileNames.add(chapter.getFileName())
                    if (viewModel.searchKey.isNullOrEmpty()) {
                        adapter.notifyItemChanged(chapter.index, true)
                    } else {
                        adapter.getItems().forEachIndexed { index, bookChapter ->
                            if (bookChapter.index == chapter.index) {
                                adapter.notifyItemChanged(index, true)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun upChapterList(searchKey: String?) {
        launch {
            withContext(IO) {
                when {
                    searchKey.isNullOrBlank() -> appDb.bookChapterDao.getChapterList(viewModel.bookUrl)
                    else -> appDb.bookChapterDao.search(viewModel.bookUrl, searchKey)
                }
            }.let {
                adapter.setItems(it)
            }
        }
    }

    override fun onListChanged() {
        launch {
            var scrollPos = 0
            withContext(Default) {
                adapter.getItems().forEachIndexed { index, bookChapter ->
                    if (bookChapter.index >= durChapterIndex) {
                        return@withContext
                    }
                    scrollPos = index
                }
            }
            mLayoutManager.scrollToPositionWithOffset(scrollPos, 0)
            adapter.upDisplayTitles(scrollPos)
        }
    }

    override fun clearDisplayTitle() {
        adapter.clearDisplayTitle()
        adapter.upDisplayTitles(mLayoutManager.findFirstVisibleItemPosition())
    }

    override val scope: CoroutineScope
        get() = this

    override val book: Book?
        get() = viewModel.bookData.value

    override val isLocalBook: Boolean
        get() = viewModel.bookData.value?.isLocalBook() == true

    override fun durChapterIndex(): Int {
        return durChapterIndex
    }

    override fun openChapter(bookChapter: BookChapter) {
        activity?.run {
            setResult(RESULT_OK, Intent().putExtra("index", bookChapter.index))
            finish()
        }
    }

}