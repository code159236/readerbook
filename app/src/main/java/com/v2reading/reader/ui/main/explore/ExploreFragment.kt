package com.v2reading.reader.ui.main.explore

import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.SubMenu
import android.view.View
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.v2reading.reader.R
import com.v2reading.reader.base.VMBaseFragment
import com.v2reading.reader.constant.AppLog
import com.v2reading.reader.constant.AppPattern
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.BookSource
import com.v2reading.reader.databinding.FragmentExploreBinding
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.help.http.newCallResponse
import com.v2reading.reader.help.http.okHttpClient
import com.v2reading.reader.help.http.text
import com.v2reading.reader.lib.dialogs.selector
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.lib.theme.primaryTextColor
import com.v2reading.reader.model.OnlineSource
import com.v2reading.reader.ui.association.ImportBookSourceDialog
import com.v2reading.reader.ui.book.explore.ExploreShowActivity
import com.v2reading.reader.ui.book.source.edit.BookSourceEditActivity
import com.v2reading.reader.utils.*
import com.v2reading.reader.utils.NetworkUtils.getRaw
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

/**
 * 站点界面
 */
class ExploreFragment : VMBaseFragment<ExploreViewModel>(R.layout.fragment_explore),
    ExploreAdapter.CallBack {

    override val viewModel by viewModels<ExploreViewModel>()
    private val binding by viewBinding(FragmentExploreBinding::bind)
    private val adapter by lazy { ExploreAdapter(requireContext(), this) }
    private val linearLayoutManager by lazy { LinearLayoutManager(context) }
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }
    private val diffItemCallBack = ExploreDiffItemCallBack()
    private val groups = linkedSetOf<String>()
    private var exploreFlowJob: Job? = null
    private var groupsMenu: SubMenu? = null
    private var json: String? = null

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        setSupportToolbar(binding.titleBar.toolbar)
        initSearchView()
        initRecyclerView()
        initGroupData()
        upExploreData()

        lifecycleScope.launch(Dispatchers.IO) {
            json  = getRaw("reader/booksource_10016.json")
            if (appDb.bookSourceDao.allCount()==0) {
                val sourceList = GSON.fromJsonArray<OnlineSource>(json).getOrDefault(arrayListOf())
                launch(Dispatchers.Main) {
                    showDialogFragment(ImportBookSourceDialog(sourceList!![0].url))
                }
            }
        }

    }

    override fun onCompatCreateOptionsMenu(menu: Menu) {
        super.onCompatCreateOptionsMenu(menu)
        menuInflater.inflate(R.menu.main_explore, menu)
        groupsMenu = menu.findItem(R.id.menu_group)?.subMenu
        upGroupsMenu()
    }


    override fun onPause() {
        super.onPause()
        searchView.clearFocus()
    }

    private fun initSearchView() {
        searchView.applyTint(primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.screen_find)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                upExploreData(newText)
                return false
            }
        })
    }

    private fun initRecyclerView() {
        binding.rvFind.setEdgeEffectColor(primaryColor)
        binding.rvFind.layoutManager = linearLayoutManager
        binding.rvFind.adapter = adapter
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {

            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (positionStart == 0) {
                    binding.rvFind.scrollToPosition(0)
                }
            }
        })
    }

    private fun initGroupData() {
        launch {
            appDb.bookSourceDao.flowExploreGroup().conflate().collect {
                groups.clear()
                it.map { group ->
                    groups.addAll(group.splitNotBlank(AppPattern.splitGroupRegex))
                }
                upGroupsMenu()
            }
        }
    }

    private fun upExploreData(searchKey: String? = null) {
        exploreFlowJob?.cancel()
        exploreFlowJob = launch {
            when {
                searchKey.isNullOrBlank() -> {
                    appDb.bookSourceDao.flowExplore()
                }
                searchKey.startsWith("group:") -> {
                    val key = searchKey.substringAfter("group:")
                    appDb.bookSourceDao.flowGroupExplore(key)
                }
                else -> {
                    appDb.bookSourceDao.flowExplore(searchKey)
                }
            }.catch {
                AppLog.put("站点界面更新数据出错", it)
            }.conflate().collect {
                binding.tvEmptyMsg.isGone = it.isNotEmpty() || searchView.query.isNotEmpty()
                adapter.setItems(it, diffItemCallBack)
            }
        }
    }

    private fun upGroupsMenu() = groupsMenu?.let { subMenu ->
        subMenu.removeGroup(R.id.menu_group_text)
        groups.sortedWith { o1, o2 ->
            o1.cnCompare(o2)
        }.forEach {
            subMenu.add(R.id.menu_group_text, Menu.NONE, Menu.NONE, it)
        }
    }

    override val scope: CoroutineScope
        get() = lifecycleScope

    override fun onCompatOptionsItemSelected(item: MenuItem) {
        super.onCompatOptionsItemSelected(item)
        if (item.groupId == R.id.menu_group_text) {
            searchView.setQuery("group:${item.title}", true)
        } else if (item.itemId == R.id.choose_source) {

            if (json.isNullOrEmpty()) {
                lifecycleScope.launch(Dispatchers.IO) {
                     json = getRaw("reader/booksource_10016.json")
                }
                var sourceList = GSON.fromJsonArray<OnlineSource>(json).getOrDefault(arrayListOf())
                launch(Dispatchers.Main) {
                    if (sourceList==null) {
                        sourceList = arrayListOf()
                    }
                    context?.selector(items = sourceList!!.map { it.title }.toList()) { _, i ->
                        showDialogFragment(ImportBookSourceDialog(sourceList!![i].url))
                    }
                }
            }else {
                val sourceList = GSON.fromJsonArray<OnlineSource>(json).getOrDefault(arrayListOf())
                launch(Dispatchers.Main) {
                    context?.selector(items = sourceList!!.map { it.title }.toList()) { _, i ->
                        showDialogFragment(ImportBookSourceDialog(sourceList[i].url))
                        Log.e("TAG", "importUrl: ${sourceList[i].url}")
                    }
                }
            }
        }
    }

    suspend fun getSource(url: String): String? {
        return okHttpClient.newCallResponse {
            url(url)
            method("GET", null)
        }.apply {
//            if (!this.isSuccessful) {
//                throw Exception(this.message)
//            }
        }.body?.text()
    }

    override fun refreshData() {
        upExploreData(searchView.query?.toString())
    }

    override fun scrollTo(pos: Int) {
        (binding.rvFind.layoutManager as LinearLayoutManager).scrollToPositionWithOffset(pos, 0)
    }

    override fun openExplore(sourceUrl: String, title: String, exploreUrl: String?) {
        if (exploreUrl.isNullOrBlank()) return
        startActivity<ExploreShowActivity> {
            putExtra("exploreName", title)
            putExtra("sourceUrl", sourceUrl)
            putExtra("exploreUrl", exploreUrl)
        }
    }

    override fun editSource(sourceUrl: String) {
        startActivity<BookSourceEditActivity> {
            putExtra("sourceUrl", sourceUrl)
        }
    }

    override fun toTop(source: BookSource) {
        viewModel.topSource(source)
    }

    fun compressExplore() {
        if (!adapter.compressExplore()) {
            if (AppConfig.isEInkMode) {
                binding.rvFind.scrollToPosition(0)
            } else {
                binding.rvFind.smoothScrollToPosition(0)
            }
        }
    }

}