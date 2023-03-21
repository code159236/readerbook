package com.v2reading.reader.ui.rss.source.manage

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseDialogFragment
import com.v2reading.reader.base.adapter.ItemViewHolder
import com.v2reading.reader.base.adapter.RecyclerAdapter
import com.v2reading.reader.constant.AppPattern
import com.v2reading.reader.data.appDb
import com.v2reading.reader.databinding.DialogEditTextBinding
import com.v2reading.reader.databinding.DialogRecyclerViewBinding
import com.v2reading.reader.databinding.ItemGroupManageBinding
import com.v2reading.reader.lib.dialogs.alert
import com.v2reading.reader.lib.theme.accentColor
import com.v2reading.reader.lib.theme.backgroundColor
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.ui.widget.recycler.VerticalDivider
import com.v2reading.reader.utils.*
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch


class GroupManageDialog : BaseDialogFragment(R.layout.dialog_recycler_view),
    Toolbar.OnMenuItemClickListener {

    private val viewModel: RssSourceViewModel by activityViewModels()
    private val binding by viewBinding(DialogRecyclerViewBinding::bind)
    private val adapter by lazy { GroupAdapter(requireContext()) }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.9f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) = binding.run {
        toolBar.setBackgroundColor(primaryColor)
        toolBar.title = getString(R.string.group_manage)
        toolBar.inflateMenu(R.menu.group_manage)
        toolBar.menu.applyTint(requireContext())
        toolBar.setOnMenuItemClickListener(this@GroupManageDialog)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.addItemDecoration(VerticalDivider(requireContext()))
        recyclerView.adapter = adapter
        tvOk.setTextColor(requireContext().accentColor)
        tvOk.visible()
        tvOk.setOnClickListener {
            dismissAllowingStateLoss()
        }
        initData()
    }

    private fun initData() {
        launch {
            appDb.rssSourceDao.flowGroup().conflate().collect {
                val groups = linkedSetOf<String>()
                it.map { group ->
                    groups.addAll(group.splitNotBlank(AppPattern.splitGroupRegex))
                }
                adapter.setItems(groups.toList())
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_add -> addGroup()
        }
        return true
    }

    @SuppressLint("InflateParams")
    private fun addGroup() {
        alert(title = getString(R.string.add_group)) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.setHint(R.string.group_name)
            }
            customView { alertBinding.root }
            yesButton {
                alertBinding.editView.text?.toString()?.let {
                    if (it.isNotBlank()) {
                        viewModel.addGroup(it)
                    }
                }
            }
            noButton()
        }.requestInputMethod()
    }

    @SuppressLint("InflateParams")
    private fun editGroup(group: String) {
        alert(title = getString(R.string.group_edit)) {
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater).apply {
                editView.setHint(R.string.group_name)
                editView.setText(group)
            }
            customView { alertBinding.root }
            yesButton {
                viewModel.upGroup(group, alertBinding.editView.text?.toString())
            }
            noButton()
        }.requestInputMethod()
    }

    private inner class GroupAdapter(context: Context) :
        RecyclerAdapter<String, ItemGroupManageBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemGroupManageBinding {
            return ItemGroupManageBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemGroupManageBinding,
            item: String,
            payloads: MutableList<Any>
        ) {
            binding.run {
                root.setBackgroundColor(context.backgroundColor)
                tvGroup.text = item
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemGroupManageBinding) {
            binding.apply {
                tvEdit.setOnClickListener {
                    getItem(holder.layoutPosition)?.let {
                        editGroup(it)
                    }
                }

                tvDel.setOnClickListener {
                    getItem(holder.layoutPosition)?.let {
                        viewModel.delGroup(it)
                    }
                }
            }
        }
    }

}