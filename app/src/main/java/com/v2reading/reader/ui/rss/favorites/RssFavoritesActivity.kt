package com.v2reading.reader.ui.rss.favorites

import android.os.Bundle
import androidx.recyclerview.widget.LinearLayoutManager
import com.v2reading.reader.base.BaseActivity
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.RssStar
import com.v2reading.reader.databinding.ActivityRssFavoritesBinding
import com.v2reading.reader.ui.rss.read.ReadRssActivity
import com.v2reading.reader.ui.widget.recycler.VerticalDivider
import com.v2reading.reader.utils.startActivity
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch


class RssFavoritesActivity : BaseActivity<ActivityRssFavoritesBinding>(),
    RssFavoritesAdapter.CallBack {

    override val binding by viewBinding(ActivityRssFavoritesBinding::inflate)
    private val adapter by lazy { RssFavoritesAdapter(this, this) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initData()
    }

    private fun initView() {
        binding.recyclerView.let {
            it.layoutManager = LinearLayoutManager(this)
            it.addItemDecoration(VerticalDivider(this))
            it.adapter = adapter
        }
    }

    private fun initData() {
        launch {
            appDb.rssStarDao.liveAll().conflate().collect {
                adapter.setItems(it)
            }
        }
    }

    override fun readRss(rssStar: RssStar) {
        startActivity<ReadRssActivity> {
            putExtra("title", rssStar.title)
            putExtra("origin", rssStar.origin)
            putExtra("link", rssStar.link)
        }
    }
}