package com.v2reading.reader.ui.book.explore

import android.os.Bundle
import androidx.activity.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import com.v2reading.reader.BuildConfig
import com.v2reading.reader.R
import com.v2reading.reader.base.VMBaseActivity
import com.v2reading.reader.data.entities.Book
import com.v2reading.reader.data.entities.SearchBook
import com.v2reading.reader.databinding.ActivityExploreShowBinding
import com.v2reading.reader.databinding.ViewLoadMoreBinding
import com.v2reading.reader.model.UpdateResponse
import com.v2reading.reader.ui.book.info.BookInfoActivity
import com.v2reading.reader.ui.widget.recycler.LoadMoreView
import com.v2reading.reader.ui.widget.recycler.VerticalDivider
import com.v2reading.reader.utils.GSON
import com.v2reading.reader.utils.NetworkUtils.getRaw
import com.v2reading.reader.utils.fromJsonObject
import com.v2reading.reader.utils.startActivity
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import constant.DownLoadBy
import constant.UiType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import listener.OnBtnClickListener
import model.UiConfig
import model.UpdateConfig
import org.apache.commons.lang3.RandomUtils
import update.UpdateAppUtils

class ExploreShowActivity : VMBaseActivity<ActivityExploreShowBinding, ExploreShowViewModel>(),
    ExploreShowAdapter.CallBack {
    override val binding by viewBinding(ActivityExploreShowBinding::inflate)
    override val viewModel by viewModels<ExploreShowViewModel>()

    private val adapter by lazy { ExploreShowAdapter(this, this) }
    private val loadMoreView by lazy { LoadMoreView(this) }
    private var isLoading = true

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        binding.titleBar.title = intent.getStringExtra("exploreName")
        initRecyclerView()
        viewModel.booksData.observe(this) { upData(it) }
        viewModel.initData(intent)
        viewModel.errorLiveData.observe(this) {
            loadMoreView.error(it)
        }

        if (RandomUtils.nextInt(0, 10) > 8) {
//            checkUpdate()
        }

    }

    private fun checkUpdate() {

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val response = getRaw("reader/v2readpro2.json")
                val updateBean: UpdateResponse? =
                    GSON.fromJsonObject<UpdateResponse>(response).getOrNull()

                if ((updateBean?.versionCode ?: 0) > BuildConfig.VERSION_CODE) {

                    launch(Dispatchers.Main) {

                        UpdateAppUtils
                            .getInstance()
                            .apkUrl(updateBean?.url ?: "")
                            .updateTitle(updateBean?.updateTitle ?: "")
                            .updateContent(updateBean?.updateContent ?: "")
                            .updateConfig(UpdateConfig().apply {
                                downloadBy = DownLoadBy.APP
                                serverVersionName = updateBean?.versionName ?: ""
                                serverVersionCode = updateBean?.versionCode ?: 0
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

                } else {
//                    toastOnUi("已是最新版本")
                }

            } catch (e: Exception) {

            }
        }

    }


    private fun initRecyclerView() {
        binding.recyclerView.addItemDecoration(VerticalDivider(this))
        binding.recyclerView.adapter = adapter
        adapter.addFooterView {
            ViewLoadMoreBinding.bind(loadMoreView)
        }
        loadMoreView.startLoad()
        loadMoreView.setOnClickListener {
            if (!isLoading) {
                loadMoreView.hasMore()
                scrollToBottom()
                isLoading = true
            }
        }
        binding.recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!recyclerView.canScrollVertically(1)) {
                    scrollToBottom()
                }
            }
        })
    }

    private fun scrollToBottom() {
        adapter.let {
            if (loadMoreView.hasMore && !isLoading) {
                viewModel.explore()
            }
        }
    }

    private fun upData(books: List<SearchBook>) {
        isLoading = false
        if (books.isEmpty() && adapter.isEmpty()) {
            loadMoreView.noMore(getString(R.string.empty))
        } else if (books.isEmpty()) {
            loadMoreView.noMore()
        } else if (adapter.getItems().contains(books.first()) && adapter.getItems()
                .contains(books.last())
        ) {
            loadMoreView.noMore()
        } else {
            adapter.addItems(books)
        }
    }

    override fun showBookInfo(book: Book) {
        startActivity<BookInfoActivity> {
            putExtra("name", book.name)
            putExtra("author", book.author)
            putExtra("bookUrl", book.bookUrl)
        }
    }
}
