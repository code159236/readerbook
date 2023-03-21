package com.v2reading.reader.ui.book.search

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.flexbox.FlexboxLayoutManager
import com.tradplus.ads.base.bean.TPAdError
import com.tradplus.ads.base.bean.TPAdInfo
import com.tradplus.ads.open.interstitial.InterstitialAdListener
import com.tradplus.ads.open.interstitial.TPInterstitial
import com.v2reading.reader.R
import com.v2reading.reader.base.VMBaseActivity
import com.v2reading.reader.constant.AppPattern
import com.v2reading.reader.constant.PreferKey
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.Book
import com.v2reading.reader.data.entities.SearchKeyword
import com.v2reading.reader.databinding.ActivityBookSearchBinding
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.lib.dialogs.alert
import com.v2reading.reader.lib.theme.*
import com.v2reading.reader.ui.book.info.BookInfoActivity
import com.v2reading.reader.ui.book.source.manage.BookSourceActivity
import com.v2reading.reader.ui.widget.recycler.LoadMoreView
import com.v2reading.reader.utils.*
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SearchActivity : VMBaseActivity<ActivityBookSearchBinding, SearchViewModel>(),
    BookAdapter.CallBack,
    HistoryKeyAdapter.CallBack,
    SearchAdapter.CallBack {

    override val binding by viewBinding(ActivityBookSearchBinding::inflate)
    override val viewModel by viewModels<SearchViewModel>()


    private val adapter by lazy { SearchAdapter(this, this) }
    private val bookAdapter by lazy {
        BookAdapter(this, this).apply {
            setHasStableIds(true)
        }
    }
    private val historyKeyAdapter by lazy {
        HistoryKeyAdapter(this, this).apply {
            setHasStableIds(true)
        }
    }
    private val loadMoreView by lazy { LoadMoreView(this) }
    private val searchView: SearchView by lazy {
        binding.titleBar.findViewById(R.id.search_view)
    }
    private var historyFlowJob: Job? = null
    private var booksFlowJob: Job? = null
    private var menu: Menu? = null
    private var precisionSearchMenuItem: MenuItem? = null
    private var groups = linkedSetOf<String>()
    private val searchFinishCallback: (isEmpty: Boolean) -> Unit = {
        if (it) {
            val searchGroup = AppConfig.searchGroup
            if (searchGroup.isNotEmpty()) {
                launch {
                    alert("搜索结果为空") {
                        setMessage("${searchGroup}分组搜索结果为空,是否切换到全部分组")
                        noButton()
                        yesButton {
                            AppConfig.searchGroup = ""
                            viewModel.searchKey = ""
                            viewModel.search(searchView.query.toString())
                        }
                    }
                }
            }
        }
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.llHistory.setBackgroundColor(backgroundColor)
        initRecyclerView()
        initSearchView()
        initOtherView()
        initData()
        receiptIntent(intent)
        viewModel.searchFinishCallback = searchFinishCallback
    }


    override fun onNewIntent(data: Intent?) {
        super.onNewIntent(data)
        receiptIntent(data)
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.book_search, menu)
        precisionSearchMenuItem = menu.findItem(R.id.menu_precision_search)
        precisionSearchMenuItem?.isChecked = getPrefBoolean(PreferKey.precisionSearch)
        this.menu = menu
        upGroupMenu()
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_precision_search -> {
                putPrefBoolean(
                    PreferKey.precisionSearch,
                    !getPrefBoolean(PreferKey.precisionSearch)
                )
                precisionSearchMenuItem?.isChecked = getPrefBoolean(PreferKey.precisionSearch)
                searchView.query?.toString()?.trim()?.let {
                    searchView.setQuery(it, true)
                }
            }
            R.id.menu_source_manage -> startActivity<BookSourceActivity>()
            else -> if (item.groupId == R.id.source_group) {
                item.isChecked = true
                if (item.title.toString() == getString(R.string.all_source)) {
                    AppConfig.searchGroup = ""
                } else {
                    AppConfig.searchGroup = item.title.toString()
                }
                searchView.query?.toString()?.trim()?.let {
                    searchView.setQuery(it, true)
                }
            }
        }
        return super.onCompatOptionsItemSelected(item)
    }

    private fun initSearchView() {
        searchView.applyTint(primaryTextColor)
        searchView.onActionViewExpanded()
        searchView.isSubmitButtonEnabled = true
        searchView.queryHint = getString(R.string.search_book_key)
        searchView.clearFocus()
        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                searchView.clearFocus()
                query?.let {
                    viewModel.saveSearchKey(query)
                    viewModel.searchKey = ""
                    viewModel.search(it)
                }
                openOrCloseHistory(false)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                if (newText.isNullOrBlank()) viewModel.stop()
                upHistory(newText)
                return false
            }
        })
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
            if (!hasFocus && searchView.query.toString().trim().isEmpty()) {
                finish()
            } else {
                openOrCloseHistory(hasFocus)
            }
        }
        openOrCloseHistory(true)
    }

    private fun initRecyclerView() {
        binding.recyclerView.setEdgeEffectColor(primaryColor)
        binding.rvBookshelfSearch.setEdgeEffectColor(primaryColor)
        binding.rvHistoryKey.setEdgeEffectColor(primaryColor)
        binding.rvBookshelfSearch.layoutManager = FlexboxLayoutManager(this)
        binding.rvBookshelfSearch.adapter = bookAdapter
        binding.rvHistoryKey.layoutManager = FlexboxLayoutManager(this)
        binding.rvHistoryKey.adapter = historyKeyAdapter
        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
        adapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                if (positionStart == 0) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                super.onItemRangeMoved(fromPosition, toPosition, itemCount)
                if (toPosition == 0) {
                    binding.recyclerView.scrollToPosition(0)
                }
            }
        })
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    scrollToBottom()
                }
            }
        })
    }

    private fun initOtherView() {
        binding.fbStop.backgroundTintList =
            Selector.colorBuild()
                .setDefaultColor(accentColor)
                .setPressedColor(ColorUtils.darkenColor(accentColor))
                .create()
        binding.fbStop.setOnClickListener {
            viewModel.stop()
            binding.refreshProgressBar.isAutoLoading = false
        }
        binding.tvClearHistory.setOnClickListener { viewModel.clearHistory() }
    }

    private fun initData() {
        lifecycleScope.launchWhenStarted {
            viewModel.searchDataFlow.conflate().collect {
                adapter.setItems(it)
                delay(1000)
            }
        }
        launch {
            appDb.bookSourceDao.flowGroupEnabled().conflate().collect {
                groups.clear()
                it.map { group ->
                    groups.addAll(group.splitNotBlank(AppPattern.splitGroupRegex))
                }
                upGroupMenu()
            }
        }
        viewModel.isSearchLiveData.observe(this) {
            if (it) {
                startSearch()
            } else {
                searchFinally()
            }
        }
    }

    private fun receiptIntent(intent: Intent? = null) {
        val key = intent?.getStringExtra("key")
        if (key.isNullOrBlank()) {
            searchView.findViewById<TextView>(androidx.appcompat.R.id.search_src_text)
                .requestFocus()
        } else {
            searchView.setQuery(key, true)
        }
    }

    /**
     * 滚动到底部事件
     */
    private fun scrollToBottom() {
        if (viewModel.isSearchLiveData.value == false
            && viewModel.searchKey.isNotEmpty()
            && loadMoreView.hasMore
        ) {
            viewModel.search("")
        }
    }

    /**
     * 打开关闭历史界面
     */
    private fun openOrCloseHistory(open: Boolean) {
        if (open) {
            upHistory(searchView.query.toString())
            binding.llHistory.visibility = VISIBLE
        } else {
            binding.llHistory.visibility = GONE
        }
    }

    /**
     * 更新分组菜单
     */
    private fun upGroupMenu() = menu?.let { menu ->
        val selectedGroup = AppConfig.searchGroup
        menu.removeGroup(R.id.source_group)
        val allItem = menu.add(R.id.source_group, Menu.NONE, Menu.NONE, R.string.all_source)
        var hasSelectedGroup = false
        groups.sortedWith { o1, o2 ->
            o1.cnCompare(o2)
        }.forEach { group ->
            menu.add(R.id.source_group, Menu.NONE, Menu.NONE, group)?.let {
                if (group == selectedGroup) {
                    it.isChecked = true
                    hasSelectedGroup = true
                }
            }
        }
        menu.setGroupCheckable(R.id.source_group, true, true)
        if (!hasSelectedGroup) {
            allItem.isChecked = true
        }
    }

    /**
     * 更新搜索历史
     */
    private fun upHistory(key: String? = null) {
        booksFlowJob?.cancel()
        booksFlowJob = launch {
            if (key.isNullOrBlank()) {
                binding.tvBookShow.gone()
                binding.rvBookshelfSearch.gone()
            } else {
                appDb.bookDao.flowSearch(key).conflate().collect {
                    if (it.isEmpty()) {
                        binding.tvBookShow.gone()
                        binding.rvBookshelfSearch.gone()
                    } else {
                        binding.tvBookShow.visible()
                        binding.rvBookshelfSearch.visible()
                    }
                    bookAdapter.setItems(it)
                }
            }
        }
        historyFlowJob?.cancel()
        historyFlowJob = launch {
            when {
                key.isNullOrBlank() -> appDb.searchKeywordDao.flowByUsage()
                else -> appDb.searchKeywordDao.flowSearch(key)
            }.conflate().collect {
                historyKeyAdapter.setItems(it)
                if (it.isEmpty()) {
                    binding.tvClearHistory.invisible()
                } else {
                    binding.tvClearHistory.visible()
                }
            }
        }
    }

    var mTPInterstitial: TPInterstitial? = null

    /**
     * 开始搜索
     */
    private fun startSearch() {
        binding.refreshProgressBar.isAutoLoading = true
        binding.fbStop.visible()

        if (mTPInterstitial == null) {
            initInterstitialAd()
        } else
            mTPInterstitial?.reloadAd()

    }

    val TAG = "Search"

    /**
     * 初始化广告位
     * 如果要开启自动加载，初始化广告位的时机要尽可能提前，这样才能保住在进入广告场景后有可用的广告
     * 如果不开启自动加载，那么初始化广告位后，在合适的时机来调用load
     */
    private fun initInterstitialAd() {
        /*
         * 1、参数2：广告位
         *
         * 2、参数3：自动reload模式，true 开启 ，false 关闭（详细请参考接入文档或者类和方法的注释）
         */
        mTPInterstitial = TPInterstitial(this, "AA4B086C4492EB877D0B6750B867C623")

        //进入广告场景，广告场景ID后台创建
        // 广告场景是用来统计进入广告场景的次数和进入场景后展示广告的次数，所以请在准确的位置调用
        mTPInterstitial!!.entryAdScenario("91D345DAF9184D6F590010A69060AF82")

//        // 流量分组的时候用到，可以自定义一些app相关的属性，在TradPlus后台根据这些属性来对用户做分组
//        // 设置流量分组有两个维度，一个是全局的，一个是单个广告位的，单个广告位的相同属性会覆盖全局的
//        val customMap: HashMap<String, String> = HashMap()
//        customMap["user_gender"] = "male" //男性
//        customMap["user_level"] = "10" //游戏等级10
//        //        SegmentUtils.initCustomMap(customMap);//设置APP维度的规则，对全部placement有效
//        SegmentUtils.initPlacementCustomMap(
//            TestAdUnitId.ENTRY_AD_INTERSTITIAL,
//            customMap
//        ) //仅对该广告位有效，会覆盖APP维度设置的规则

        // 监听广告的不同状态
        mTPInterstitial!!.setAdListener(object : InterstitialAdListener {
            override fun onAdLoaded(tpAdInfo: TPAdInfo?) {
                Log.i(TAG, "onAdLoaded: ")
                if (mTPInterstitial?.isReady == true) {
                    mTPInterstitial?.showAd(this@SearchActivity, "91D345DAF9184D6F590010A69060AF82")
                }
            }

            override fun onAdClicked(tpAdInfo: TPAdInfo) {
                Log.i(TAG, "onAdClicked: 广告" + tpAdInfo.adSourceName + "被点击")
            }

            override fun onAdImpression(tpAdInfo: TPAdInfo) {
                Log.i(TAG, "onAdImpression: 广告" + tpAdInfo.adSourceName + "展示")
            }

            override fun onAdFailed(tpAdError: TPAdError?) {
                Log.i(TAG, "onAdFailed: ")
            }

            override fun onAdClosed(tpAdInfo: TPAdInfo) {
                Log.i(TAG, "onAdClosed: 广告" + tpAdInfo.adSourceName + "被关闭")
            }

            override fun onAdVideoError(tpAdInfo: TPAdInfo, tpAdError: TPAdError?) {
                Log.i(TAG, "onAdClosed: 广告" + tpAdInfo.adSourceName + "展示失败")
            }

            override fun onAdVideoStart(tpAdInfo: TPAdInfo?) {
                // V8.1.0.1 播放开始
            }

            override fun onAdVideoEnd(tpAdInfo: TPAdInfo?) {
                // V8.1.0.1 播放结束
            }
        })

        mTPInterstitial?.loadAd()
    }

    override fun onDestroy() {
        super.onDestroy()
        if (mTPInterstitial != null) {
            mTPInterstitial!!.onDestroy()
        }
    }

    /**
     * 搜索结束
     */
    private fun searchFinally() {
        binding.refreshProgressBar.isAutoLoading = false
        loadMoreView.startLoad()
        binding.fbStop.invisible()
    }

    /**
     * 显示书籍详情
     */
    override fun showBookInfo(name: String, author: String) {
        startActivity<BookInfoActivity> {
            putExtra("name", name)
            putExtra("author", author)
        }
    }

    /**
     * 显示书籍详情
     */
    override fun showBookInfo(book: Book) {
        showBookInfo(book.name, book.author)
    }

    /**
     * 点击历史关键字
     */
    override fun searchHistory(key: String) {
        launch {
            when {
                searchView.query.toString() == key -> {
                    searchView.setQuery(key, true)
                }
                withContext(IO) { appDb.bookDao.findByName(key).isEmpty() } -> {
                    searchView.setQuery(key, true)
                }
                else -> {
                    searchView.setQuery(key, false)
                }
            }
        }
    }

    override fun deleteHistory(searchKeyword: SearchKeyword) {
        viewModel.deleteHistory(searchKeyword)
    }


    companion object {

        fun start(context: Context, key: String?) {
            context.startActivity<SearchActivity> {
                putExtra("key", key)
            }
        }

    }
}