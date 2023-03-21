package com.v2reading.reader.ui.book.local.rule

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import androidx.recyclerview.widget.ItemTouchHelper
import com.v2reading.reader.R
import com.v2reading.reader.base.VMBaseActivity
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.TxtTocRule
import com.v2reading.reader.databinding.ActivityTxtTocRuleBinding
import com.v2reading.reader.databinding.DialogEditTextBinding
import com.v2reading.reader.databinding.DialogTocRegexEditBinding
import com.v2reading.reader.lib.dialogs.alert
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.ui.widget.SelectActionBar
import com.v2reading.reader.ui.widget.recycler.DragSelectTouchHelper
import com.v2reading.reader.ui.widget.recycler.ItemTouchCallback
import com.v2reading.reader.ui.widget.recycler.VerticalDivider
import com.v2reading.reader.utils.ACache
import com.v2reading.reader.utils.setEdgeEffectColor
import com.v2reading.reader.utils.splitNotBlank
import com.v2reading.reader.utils.toastOnUi
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

class TxtTocRuleActivity : VMBaseActivity<ActivityTxtTocRuleBinding, TxtTocRuleViewModel>(),
    TxtTocRuleAdapter.CallBack,
    SelectActionBar.CallBack {

    override val viewModel: TxtTocRuleViewModel by viewModels()
    override val binding: ActivityTxtTocRuleBinding by viewBinding(ActivityTxtTocRuleBinding::inflate)
    private val adapter: TxtTocRuleAdapter by lazy {
        TxtTocRuleAdapter(this, this)
    }
    private val importTocRuleKey = "tocRuleUrl"

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        initView()
        initBottomActionBar()
        initData()
    }

    private fun initView() = binding.run {
        recyclerView.setEdgeEffectColor(primaryColor)
        recyclerView.addItemDecoration(VerticalDivider(this@TxtTocRuleActivity))
        recyclerView.adapter = adapter
        // When this page is opened, it is in selection mode
        val dragSelectTouchHelper =
            DragSelectTouchHelper(adapter.dragSelectCallback).setSlideArea(16, 50)
        dragSelectTouchHelper.attachToRecyclerView(binding.recyclerView)
        dragSelectTouchHelper.activeSlideSelect()
        // Note: need judge selection first, so add ItemTouchHelper after it.
        val itemTouchCallback = ItemTouchCallback(adapter)
        itemTouchCallback.isCanDrag = true
        ItemTouchHelper(itemTouchCallback).attachToRecyclerView(binding.recyclerView)
    }

    private fun initBottomActionBar() {
        binding.selectActionBar.setMainActionText(R.string.delete)
        binding.selectActionBar.setCallBack(this)
    }

    private fun initData() {
        launch {
            appDb.txtTocRuleDao.observeAll().conflate().collect { tocRules ->
                adapter.setItems(tocRules)
                upCountView()
            }
        }
    }

    override fun onCompatCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.txt_toc_regex, menu)
        return super.onCompatCreateOptionsMenu(menu)
    }

    override fun onCompatOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_add -> edit(TxtTocRule())
            R.id.menu_default -> viewModel.importDefault()
            R.id.menu_import -> showImportDialog()
        }
        return super.onCompatOptionsItemSelected(item)
    }

    override fun del(source: TxtTocRule) {
        viewModel.del(source)
    }

    override fun edit(source: TxtTocRule) {
        alert(titleResource = R.string.txt_toc_regex) {
            val alertBinding = DialogTocRegexEditBinding.inflate(layoutInflater)
            alertBinding.apply {
                tvRuleName.setText(source.name)
                tvRuleRegex.setText(source.rule)
            }
            customView { alertBinding.root }
            okButton {
                alertBinding.apply {
                    source.name = tvRuleName.text.toString()
                    source.rule = tvRuleRegex.text.toString()
                    viewModel.save(source)
                }
            }
            cancelButton()
        }
    }

    override fun onClickSelectBarMainAction() {
        delSourceDialog()
    }

    override fun revertSelection() {
        adapter.revertSelection()
    }

    override fun selectAll(selectAll: Boolean) {
        if (selectAll) {
            adapter.selectAll()
        } else {
            adapter.revertSelection()
        }
    }

    override fun update(vararg source: TxtTocRule) {
        viewModel.update(*source)
    }

    override fun toTop(source: TxtTocRule) {
        viewModel.toTop(source)
    }

    override fun toBottom(source: TxtTocRule) {
        viewModel.toBottom(source)
    }

    override fun upOrder() {
        viewModel.upOrder()
    }

    override fun upCountView() {
        binding.selectActionBar
            .upCountView(adapter.selection.size, adapter.itemCount)
    }

    private fun delSourceDialog() {
        alert(titleResource = R.string.draw, messageResource = R.string.sure_del) {
            okButton { viewModel.del(*adapter.selection.toTypedArray()) }
            noButton()
        }
    }

    @SuppressLint("InflateParams")
    private fun showImportDialog() {
        val aCache = ACache.get(this, cacheDir = false)
        val defaultUrl = "https://gitee.com/fisher52/YueDuJson/raw/master/myTxtChapterRule.json"
        val cacheUrls: MutableList<String> = aCache
            .getAsString(importTocRuleKey)
            ?.splitNotBlank(",")
            ?.toMutableList()
            ?: mutableListOf()
        if (!cacheUrls.contains(defaultUrl)) {
            cacheUrls.add(0, defaultUrl)
        }
        alert(titleResource = R.string.import_on_line) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.hint = "url"
                editView.setFilterValues(cacheUrls)
                editView.delCallBack = {
                    cacheUrls.remove(it)
                    aCache.put(importTocRuleKey, cacheUrls.joinToString(","))
                }
            }
            customView { alertBinding.root }
            okButton {
                val text = alertBinding.editView.text?.toString()
                text?.let {
                    if (!cacheUrls.contains(it)) {
                        cacheUrls.add(0, it)
                        aCache.put(importTocRuleKey, cacheUrls.joinToString(","))
                    }
                    viewModel.importOnLine(it) { msg ->
                        toastOnUi(msg)
                    }
                }
            }
            cancelButton()
        }
    }

}