package com.v2reading.reader.ui.book.cache

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import com.v2reading.reader.R
import com.v2reading.reader.base.adapter.ItemViewHolder
import com.v2reading.reader.base.adapter.RecyclerAdapter
import com.v2reading.reader.data.entities.Book
import com.v2reading.reader.databinding.ItemDownloadBinding
import com.v2reading.reader.model.CacheBook
import com.v2reading.reader.utils.gone
import com.v2reading.reader.utils.visible

class CacheAdapter(context: Context, private val callBack: CallBack) :
    RecyclerAdapter<Book, ItemDownloadBinding>(context) {

    val cacheChapters = hashMapOf<String, HashSet<String>>()

    override fun getViewBinding(parent: ViewGroup): ItemDownloadBinding {
        return ItemDownloadBinding.inflate(inflater, parent, false)
    }

    override fun convert(
        holder: ItemViewHolder,
        binding: ItemDownloadBinding,
        item: Book,
        payloads: MutableList<Any>
    ) {
        binding.run {
            if (payloads.isEmpty()) {
                tvName.text = item.name
                tvAuthor.text = context.getString(R.string.author_show, item.getRealAuthor())
                if (item.isLocalBook()) {
                    tvDownload.setText(R.string.local_book)
                } else {
                    val cs = cacheChapters[item.bookUrl]
                    if (cs == null) {
                        tvDownload.setText(R.string.loading)
                    } else {
                        tvDownload.text =
                            context.getString(
                                R.string.download_count,
                                cs.size,
                                item.totalChapterNum
                            )
                    }
                }
            } else {
                if (item.isLocalBook()) {
                    tvDownload.setText(R.string.local_book)
                } else {
                    val cacheSize = cacheChapters[item.bookUrl]?.size ?: 0
                    tvDownload.text =
                        context.getString(R.string.download_count, cacheSize, item.totalChapterNum)
                }
            }
            upDownloadIv(ivDownload, item)
            upExportInfo(tvMsg, progressExport, item)
        }
    }

    override fun registerListener(holder: ItemViewHolder, binding: ItemDownloadBinding) {
        binding.run {
            ivDownload.setOnClickListener {
                getItem(holder.layoutPosition)?.let { book ->
                    CacheBook.cacheBookMap[book.bookUrl]?.let {
                        if (it.isRun()) {
                            CacheBook.remove(context, book.bookUrl)
                        } else {
                           startCache(book)
                        }
                    } ?: startCache(book)
                }
            }
            tvExport.setOnClickListener {
                callBack.export(holder.layoutPosition)
            }
        }
    }

    private fun startCache(book: Book){
        CacheBook.start(context, book, 0, book.totalChapterNum)
        callBack.onStartCache()
    }

    private fun upDownloadIv(iv: ImageView, book: Book) {
        if (book.isLocalBook()) {
            iv.gone()
        } else {
            iv.visible()
            CacheBook.cacheBookMap[book.bookUrl]?.let {
                if (it.isRun()) {
                    iv.setImageResource(R.drawable.ic_stop_black_24dp)
                } else {
                    iv.setImageResource(R.drawable.ic_play_24dp)
                }
            } ?: let {
                iv.setImageResource(R.drawable.ic_play_24dp)
            }
        }
    }

    private fun upExportInfo(msgView: TextView, progressView: ProgressBar, book: Book) {
        val msg = callBack.exportMsg(book.bookUrl)
        if (msg != null) {
            msgView.text = msg
            msgView.visible()
            progressView.gone()
            return
        }
        msgView.gone()
        val progress = callBack.exportProgress(book.bookUrl)
        if (progress != null) {
            progressView.max = book.totalChapterNum
            progressView.progress = progress
            progressView.visible()
            return
        }
        progressView.gone()
    }

    interface CallBack {
        fun export(position: Int)
        fun exportProgress(bookUrl: String): Int?
        fun exportMsg(bookUrl: String): String?
        fun onStartCache()
    }
}