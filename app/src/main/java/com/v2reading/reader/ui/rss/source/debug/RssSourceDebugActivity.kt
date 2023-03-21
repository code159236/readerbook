package com.v2reading.reader.ui.rss.source.debug

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.SearchView
import androidx.activity.viewModels
import com.v2reading.reader.R
import com.v2reading.reader.base.VMBaseActivity
import com.v2reading.reader.databinding.ActivitySourceDebugBinding
import com.v2reading.reader.lib.theme.accentColor
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.ui.widget.dialog.TextDialog
import com.v2reading.reader.utils.gone
import com.v2reading.reader.utils.setEdgeEffectColor
import com.v2reading.reader.utils.showDialogFragment
import com.v2reading.reader.utils.toastOnUi
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.launch


class RssSourceDebugActivity : VMBaseActivity<ActivitySourceDebugBinding, RssSourceDebugModel>() {

    override val binding by viewBinding(ActivitySourceDebugBinding::inflate)
    override val viewModel by viewModels<RssSourceDebugModel>()

    private val adapter by lazy { RssSourceDebugAdapter(this) }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initRecyclerView()
        initSearchView()
        viewModel.observe { state, msg ->
            launch {
                adapter.addItem(msg)
                if (state == -1 || state == 1000) {
                    binding.rotateLoading.hide()
                }
            }
        }
        viewModel.initData(intent.getStringExtra("key")) {
            startSearch()
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.rss_source_debug, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_list_src -> showDialogFragment(TextDialog(viewModel.listSrc))
            R.id.menu_content_src -> showDialogFragment(TextDialog(viewModel.contentSrc))
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initRecyclerView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.recyclerView.adapter = adapter
        binding.rotateLoading.loadingColor = accentColor
    }

    private fun initSearchView() {
        binding.titleBar.findViewById<SearchView>(R.id.search_view).gone()
    }

    private fun startSearch() {
        adapter.clearItems()
        viewModel.startDebug({
            binding.rotateLoading.show()
        }, {
            toastOnUi("未获取到源")
        })
    }
}