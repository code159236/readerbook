package com.v2reading.reader.ui.about

import android.content.Context
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseActivity
import com.v2reading.reader.base.adapter.ItemViewHolder
import com.v2reading.reader.base.adapter.RecyclerAdapter
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.ReadRecordShow
import com.v2reading.reader.databinding.ActivityReadRecordBinding
import com.v2reading.reader.databinding.ItemReadRecordBinding
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.lib.dialogs.alert
import com.v2reading.reader.ui.book.read.ReadBookActivity
import com.v2reading.reader.ui.book.search.SearchActivity
import com.v2reading.reader.utils.cnCompare
import com.v2reading.reader.utils.startActivity
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ReadRecordActivity : BaseActivity<ActivityReadRecordBinding>() {

    private val adapter by lazy { RecordAdapter(this) }
    private var sortMode = 0

    override val binding by viewBinding(ActivityReadRecordBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_read_record, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onMenuOpened(featureId: Int, menu: Menu): Boolean {
        menu.findItem(R.id.menu_enable_record)?.isChecked = AppConfig.enableReadRecord
        return super.onMenuOpened(featureId, menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_sort_name -> {
                sortMode = 0
                initData()
            }
            R.id.menu_sort_time -> {
                sortMode = 1
                initData()
            }
            R.id.menu_enable_record -> {
                AppConfig.enableReadRecord = !item.isChecked
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initView() = binding.run {
        readRecord.tvBookName.setText(R.string.all_read_time)
        recyclerView.adapter = adapter
        readRecord.tvRemove.setOnClickListener {
            alert(R.string.delete, R.string.sure_del) {
                yesButton {
                    appDb.readRecordDao.clear()
                    initData()
                }
                noButton()
            }
        }
    }

    private fun initData() {
        launch {
            val allTime = withContext(IO) {
                appDb.readRecordDao.allTime
            }
            binding.readRecord.tvReadTime.text = formatDuring(allTime)
            val readRecords = withContext(IO) {
                appDb.readRecordDao.allShow.let { records ->
                    when (sortMode) {
                        1 -> records.sortedBy { it.readTime }
                        else -> records.sortedWith { o1, o2 ->
                            o1.bookName.cnCompare(o2.bookName)
                        }
                    }
                }
            }
            adapter.setItems(readRecords)
        }
    }

    inner class RecordAdapter(context: Context) :
        RecyclerAdapter<ReadRecordShow, ItemReadRecordBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemReadRecordBinding {
            return ItemReadRecordBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemReadRecordBinding,
            item: ReadRecordShow,
            payloads: MutableList<Any>
        ) {
            binding.apply {
                tvBookName.text = item.bookName
                tvReadTime.text = formatDuring(item.readTime)
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemReadRecordBinding) {
            binding.apply {
                root.setOnClickListener {
                    val item = getItem(holder.layoutPosition) ?: return@setOnClickListener
                    launch {
                        val book = withContext(IO) {
                            appDb.bookDao.findByName(item.bookName).firstOrNull()
                        }
                        if (book == null) {
                            SearchActivity.start(this@ReadRecordActivity, item.bookName)
                        } else {
                            startActivity<ReadBookActivity> {
                                putExtra("bookUrl", book.bookUrl)
                            }
                        }
                    }
                }
                tvRemove.setOnClickListener {
                    getItem(holder.layoutPosition)?.let { item ->
                        sureDelAlert(item)
                    }
                }
            }
        }

        private fun sureDelAlert(item: ReadRecordShow) {
            alert(R.string.delete) {
                setMessage(getString(R.string.sure_del_any, item.bookName))
                yesButton {
                    appDb.readRecordDao.deleteByName(item.bookName)
                    initData()
                }
                noButton()
            }
        }

    }

    fun formatDuring(mss: Long): String {
        val days = mss / (1000 * 60 * 60 * 24)
        val hours = mss % (1000 * 60 * 60 * 24) / (1000 * 60 * 60)
        val minutes = mss % (1000 * 60 * 60) / (1000 * 60)
        val seconds = mss % (1000 * 60) / 1000
        val d = if (days > 0) "${days}天" else ""
        val h = if (hours > 0) "${hours}小时" else ""
        val m = if (minutes > 0) "${minutes}分钟" else ""
        val s = if (seconds > 0) "${seconds}秒" else ""
        var time = "$d$h$m$s"
        if (time.isBlank()) {
            time = "0秒"
        }
        return time
    }

}