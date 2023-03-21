package com.v2reading.reader.ui.main.bookshelf.style1.books

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.os.Bundle
import android.view.View
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.core.view.isGone
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.v2reading.reader.BuildConfig
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseFragment
import com.v2reading.reader.constant.*
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.Book
import com.v2reading.reader.databinding.FragmentBooksBinding
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.lib.theme.accentColor
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.model.BannerBean
import com.v2reading.reader.model.UpdateResponse
import com.v2reading.reader.ui.book.audio.AudioPlayActivity
import com.v2reading.reader.ui.book.info.BookInfoActivity
import com.v2reading.reader.ui.book.read.ReadBookActivity
import com.v2reading.reader.ui.book.search.SearchActivity
import com.v2reading.reader.ui.main.MainActivity
import com.v2reading.reader.ui.main.MainViewModel
import com.v2reading.reader.utils.*
import com.v2reading.reader.utils.NetworkUtils.getRaw
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import com.zhpan.bannerview.BannerViewPager
import constant.DownLoadBy
import constant.UiType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import listener.OnBtnClickListener
import model.UiConfig
import model.UpdateConfig
import update.UpdateAppUtils
import kotlin.math.max

/**
 * 书架界面
 */
class BooksFragment() : BaseFragment(R.layout.fragment_books),
    BaseBooksAdapter.CallBack {

    constructor(position: Int, groupId: Long) : this() {
        val bundle = Bundle()
        bundle.putInt("position", position)
        bundle.putLong("groupId", groupId)
        arguments = bundle
    }

    private val binding by viewBinding(FragmentBooksBinding::bind)
    private val activityViewModel by activityViewModels<MainViewModel>()
    private val bookshelfLayout by lazy {
        getPrefInt(PreferKey.bookshelfLayout)
    }
    private val booksAdapter: BaseBooksAdapter<*> by lazy {
        if (bookshelfLayout == 0) {
            BooksAdapterList(requireContext(), this)
        } else {
            BooksAdapterGrid(requireContext(), this)
        }
    }
    private var booksFlowJob: Job? = null
    private var savedInstanceState: Bundle? = null
    private var position = 0
    private var groupId = -1L
    private lateinit var mViewPager: BannerViewPager<BannerBean>

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        this.savedInstanceState = savedInstanceState
        arguments?.let {
            position = it.getInt("position", 0)
            groupId = it.getLong("groupId", -1)
        }
        initRecyclerView()
        upRecyclerData()
        setupViewPager()
    }

    private fun setupViewPager() {
        mViewPager = binding.bannerView as BannerViewPager<BannerBean>
        mViewPager.apply {
            adapter = SimpleAdapter()
            setLifecycleRegistry(lifecycle)
            setOnPageClickListener { clickedView, position ->
                run {
                    val bean = data[position]
                    when (bean.action) {
                        "update" -> {
                            checkUpdate()
                        }
                        "search" -> {
                            SearchActivity.start(requireContext(), bean.des)
                        }
                        else -> {
                            bean.action?.let { launchUrl(it) }
                        }
                    }
                }
            }
        }.create()

        getBanner()
    }

    private fun getBanner() {
        lifecycleScope.launch(Dispatchers.IO) {
            val data = getRaw("reader/banner_config2.json")
            val list = GSON.fromJsonArray<BannerBean>(data).getOrDefault(arrayListOf())
            launch(Dispatchers.Main) {
                mViewPager.refreshData(list)
            }

        }
    }

    private val customTabsIntent by lazy {
        CustomTabsIntent.Builder().apply {
            setColorScheme(CustomTabsIntent.COLOR_SCHEME_SYSTEM)
            setColorSchemeParams(
                CustomTabsIntent.COLOR_SCHEME_LIGHT,
                CustomTabColorSchemeParams.Builder().apply {
                    setToolbarColor(
                        ContextCompat.getColor(
                            context!!,
                            R.color.primary
                        )
                    )
                }.build()
            )
            setColorSchemeParams(
                CustomTabsIntent.COLOR_SCHEME_DARK,
                CustomTabColorSchemeParams.Builder().apply {
                    setToolbarColor(
                        ContextCompat.getColor(
                            context!!,
                            R.color.primary
                        )
                    )
                }.build()
            )
        }.build()
    }

    private fun launchUrl(uri: String) = try {
        customTabsIntent.launchUrl(context!!, uri.toUri())
    } catch (_: ActivityNotFoundException) {

    }

    private fun checkUpdate() {

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = getRaw("reader/v2readpro2.json")
                val updateBean: UpdateResponse? =
                    GSON.fromJsonObject<UpdateResponse>(response).getOrNull()

                launch(Dispatchers.Main) {

                    UpdateAppUtils
                        .getInstance()
                        .apkUrl(updateBean?.url ?: "")
                        .updateTitle(updateBean?.updateTitle ?: "")
                        .updateContent(updateBean?.updateContent ?: "")
                        .updateConfig(UpdateConfig().apply {
                            downloadBy = DownLoadBy.APP
                            serverVersionName = updateBean?.versionName ?: ""
                            serverVersionCode = 99999999
                            checkWifi = true
                            force =
                                updateBean?.forceVersionCode!! > BuildConfig.VERSION_CODE
                        })
                        .uiConfig(
                            UiConfig(
                                uiType = UiType.PLENTIFUL,
                                updateLogoImgRes = R.mipmap.ic_launcher
                            )
                        )
                        // 设置 取消 按钮点击事件
                        .setCancelBtnClickListener(object : OnBtnClickListener {
                            override fun onClick(): Boolean {

                                return false // 事件是否消费，是否需要传递下去。false-会执行原有点击逻辑，true-只执行本次设置的点击逻辑
                            }
                        })
                        // 设置 立即更新 按钮点击事件
                        .setUpdateBtnClickListener(object : OnBtnClickListener {
                            override fun onClick(): Boolean {

                                return false // 事件是否消费，是否需要传递下去。false-会执行原有点击逻辑，true-只执行本次设置的点击逻辑
                            }
                        })
                        .update()
                }

            } catch (e: Exception) {

            }
        }

    }

    private fun initRecyclerView() {
        binding.rvBookshelf.setEdgeEffectColor(primaryColor)
        binding.refreshLayout.setColorSchemeColors(accentColor)
        binding.refreshLayout.setOnRefreshListener {
            binding.refreshLayout.isRefreshing = false
            activityViewModel.upToc(booksAdapter.getItems())
        }
        if (bookshelfLayout == 0) {
            binding.rvBookshelf.layoutManager = LinearLayoutManager(context)
        } else {
            binding.rvBookshelf.layoutManager = GridLayoutManager(context, bookshelfLayout + 2)
        }
        binding.rvBookshelf.adapter = booksAdapter
        booksAdapter.registerAdapterDataObserver(object : RecyclerView.AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                val layoutManager = binding.rvBookshelf.layoutManager
                if (positionStart == 0 && layoutManager is LinearLayoutManager) {
                    val scrollTo = layoutManager.findFirstVisibleItemPosition() - itemCount
                    binding.rvBookshelf.scrollToPosition(max(0, scrollTo))
                }
            }

            override fun onItemRangeMoved(fromPosition: Int, toPosition: Int, itemCount: Int) {
                val layoutManager = binding.rvBookshelf.layoutManager
                if (toPosition == 0 && layoutManager is LinearLayoutManager) {
                    val scrollTo = layoutManager.findFirstVisibleItemPosition() - itemCount
                    binding.rvBookshelf.scrollToPosition(max(0, scrollTo))
                }
            }
        })

        binding.add.setOnClickListener {
            (activity as MainActivity).startAddBook()
        }

    }

    private fun upRecyclerData() {
        booksFlowJob?.cancel()
        booksFlowJob = launch {
            when (groupId) {
                AppConst.bookGroupAllId -> appDb.bookDao.flowAll()
                AppConst.bookGroupLocalId -> appDb.bookDao.flowLocal()
                AppConst.bookGroupAudioId -> appDb.bookDao.flowAudio()
                AppConst.bookGroupNoneId -> appDb.bookDao.flowNoGroup()
                else -> appDb.bookDao.flowByGroup(groupId)
            }.conflate().map { list ->
                when (getPrefInt(PreferKey.bookshelfSort)) {
                    1 -> list.sortedByDescending { it.latestChapterTime }
                    2 -> list.sortedWith { o1, o2 ->
                        o1.name.cnCompare(o2.name)
                    }
                    3 -> list.sortedBy { it.order }
                    else -> list.sortedByDescending { it.durChapterTime }
                }
            }.flowOn(Dispatchers.Default).catch {
                AppLog.put("书架更新出错", it)
            }.conflate().collect { list ->
                binding.tvEmptyMsg.isGone = true
                booksAdapter.setItems(list)
                binding.add.isGone = false
                recoverPositionState()
                delay(100)
            }
        }
    }

    private fun recoverPositionState() {
        // 恢复书架位置状态
        if (savedInstanceState?.getBoolean("needRecoverState") == true) {
            val layoutManager = binding.rvBookshelf.layoutManager
            if (layoutManager is LinearLayoutManager) {
                val leavePosition = savedInstanceState!!.getInt("leavePosition")
                val leaveOffset = savedInstanceState!!.getInt("leaveOffset")
                layoutManager.scrollToPositionWithOffset(leavePosition, leaveOffset)
            }
            savedInstanceState!!.putBoolean("needRecoverState", false)
        }
    }

    fun getBooks(): List<Book> {
        return booksAdapter.getItems()
    }

    fun gotoTop() {
        if (AppConfig.isEInkMode) {
            binding.rvBookshelf.scrollToPosition(0)
        } else {
            binding.rvBookshelf.smoothScrollToPosition(0)
        }
    }

    fun getBooksCount(): Int {
        return booksAdapter.itemCount
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // 保存书架位置状态
        val layoutManager = binding.rvBookshelf.layoutManager
        if (layoutManager is LinearLayoutManager) {
            val itemPosition = layoutManager.findFirstVisibleItemPosition()
            val currentView = layoutManager.findViewByPosition(itemPosition)
            val viewOffset = currentView?.top
            if (viewOffset != null) {
                outState.putInt("leavePosition", itemPosition)
                outState.putInt("leaveOffset", viewOffset)
                outState.putBoolean("needRecoverState", true)
            } else if (savedInstanceState != null) {
                val leavePosition = savedInstanceState!!.getInt("leavePosition")
                val leaveOffset = savedInstanceState!!.getInt("leaveOffset")
                outState.putInt("leavePosition", leavePosition)
                outState.putInt("leaveOffset", leaveOffset)
                outState.putBoolean("needRecoverState", true)
            }
        }
    }

    override fun open(book: Book) {
        when (book.type) {
            BookType.audio ->
                startActivity<AudioPlayActivity> {
                    putExtra("bookUrl", book.bookUrl)
                }
            else -> startActivity<ReadBookActivity> {
                putExtra("bookUrl", book.bookUrl)
            }
        }
    }

    override fun openBookInfo(book: Book) {
        startActivity<BookInfoActivity> {
            putExtra("name", book.name)
            putExtra("author", book.author)
        }
    }

    override fun isUpdate(bookUrl: String): Boolean {
        return activityViewModel.isUpdate(bookUrl)
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<String>(EventBus.UP_BOOKSHELF) {
            booksAdapter.notification(it)
        }
        observeEvent<String>(EventBus.BOOKSHELF_REFRESH) {
            booksAdapter.notifyDataSetChanged()
        }
    }
}