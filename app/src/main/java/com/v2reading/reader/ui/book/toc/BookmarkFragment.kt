package com.v2reading.reader.ui.book.toc

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.fragment.app.activityViewModels
import com.v2reading.reader.R
import com.v2reading.reader.base.VMBaseFragment
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.Bookmark
import com.v2reading.reader.databinding.FragmentBookmarkBinding
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.ui.book.bookmark.BookmarkDialog
import com.v2reading.reader.ui.widget.recycler.UpLinearLayoutManager
import com.v2reading.reader.ui.widget.recycler.VerticalDivider
import com.v2reading.reader.utils.setEdgeEffectColor
import com.v2reading.reader.utils.showDialogFragment
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class BookmarkFragment : VMBaseFragment<TocViewModel>(R.layout.fragment_bookmark),
    BookmarkAdapter.Callback,
    BookmarkDialog.Callback,
    TocViewModel.BookmarkCallBack {
    override val viewModel by activityViewModels<TocViewModel>()
    private val binding by viewBinding(FragmentBookmarkBinding::bind)
    private val mLayoutManager by lazy { UpLinearLayoutManager(requireContext()) }
    private val adapter by lazy { BookmarkAdapter(requireContext(), this) }
    private var durChapterIndex = 0

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.bookMarkCallBack = this
        initRecyclerView()
        viewModel.bookData.observe(this) {
            durChapterIndex = it.durChapterIndex
            upBookmark(null)
        }
    }

    private fun initRecyclerView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.layoutManager = mLayoutManager
        binding.recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        binding.recyclerView.adapter = adapter
    }

    override fun upBookmark(searchKey: String?) {
        val book = viewModel.bookData.value ?: return
        launch {
            withContext(IO) {
                when {
                    searchKey.isNullOrBlank() -> appDb.bookmarkDao.getByBook(book.name, book.author)
                    else -> appDb.bookmarkDao.search(book.name, book.author, searchKey)
                }
            }.let {
                adapter.setItems(it)
                var scrollPos = 0
                withContext(Dispatchers.Default) {
                    adapter.getItems().forEachIndexed { index, bookmark ->
                        if (bookmark.chapterIndex >= durChapterIndex) {
                            return@withContext
                        }
                        scrollPos = index
                    }
                }
                mLayoutManager.scrollToPositionWithOffset(scrollPos, 0)
            }
        }
    }


    override fun onClick(bookmark: Bookmark) {
        activity?.run {
            setResult(Activity.RESULT_OK, Intent().apply {
                putExtra("index", bookmark.chapterIndex)
                putExtra("chapterPos", bookmark.chapterPos)
            })
            finish()
        }
    }

    override fun onLongClick(bookmark: Bookmark, pos: Int) {
        showDialogFragment(BookmarkDialog(bookmark, pos))
    }

    override fun upBookmark(pos: Int, bookmark: Bookmark) {
        adapter.setItem(pos, bookmark)
    }

    override fun deleteBookmark(pos: Int) {
        adapter.removeItem(pos)
    }
}