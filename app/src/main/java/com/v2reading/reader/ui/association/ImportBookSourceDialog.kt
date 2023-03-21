package com.v2reading.reader.ui.association

import android.annotation.SuppressLint
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseDialogFragment
import com.v2reading.reader.base.adapter.ItemViewHolder
import com.v2reading.reader.base.adapter.RecyclerAdapter
import com.v2reading.reader.constant.AppPattern
import com.v2reading.reader.constant.PreferKey
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.BookSource
import com.v2reading.reader.databinding.DialogCustomGroupBinding
import com.v2reading.reader.databinding.DialogImportBooksourceRecyclerViewBinding
import com.v2reading.reader.databinding.ItemSourceImportBinding
import com.v2reading.reader.lib.dialogs.alert
import com.v2reading.reader.ui.widget.dialog.CodeDialog
import com.v2reading.reader.ui.widget.dialog.WaitDialog
import com.v2reading.reader.utils.*
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import splitties.views.onClick


/**
 * 导入站点弹出窗口
 */
class ImportBookSourceDialog() :
    BaseDialogFragment(R.layout.dialog_import_booksource_recycler_view),
    Toolbar.OnMenuItemClickListener,
    CodeDialog.Callback {

    constructor(source: String, finishOnDismiss: Boolean = false) : this() {
        arguments = Bundle().apply {
            putString("source", source)
            putBoolean("finishOnDismiss", finishOnDismiss)
        }
    }

    private val binding by viewBinding(DialogImportBooksourceRecyclerViewBinding::bind)
    private val viewModel by viewModels<ImportBookSourceViewModel>()
    private val adapter by lazy { SourcesAdapter(requireContext()) }

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        if (arguments?.getBoolean("finishOnDismiss") == true) {
            activity?.finish()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {

        binding.rotateLoading.show()

        viewModel.errorLiveData.observe(this) {
            binding.rotateLoading.hide()
            dismissAllowingStateLoss()
            toastOnUi("自动拉取站点分组失败,请在站点页面手动选取站点分组")

        }
        viewModel.successLiveData.observe(this) {
            binding.rotateLoading.hide()
            if (it > 0) {
                adapter.setItems(viewModel.allSources)
            }

            val waitDialog = WaitDialog(requireContext())
            waitDialog.show()
            viewModel.importSelect {
                waitDialog.dismiss()
                dismissAllowingStateLoss()
            }
        }

        val source = arguments?.getString("source")
        if (source.isNullOrEmpty()) {
            dismiss()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            viewModel.importSource(NetworkUtils.getRaw(source))
        }

    }


    override fun onResume() {
        super.onResume()
        dialog?.window?.setLayout(
            context!!.resources.getDimensionPixelSize(R.dimen.dp_120),
            context!!.resources.getDimensionPixelSize(R.dimen.dp_120)
        )
    }

    @SuppressLint("InflateParams")
    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_new_group -> alertCustomGroup(item)
            R.id.menu_Keep_original_name -> {
                item.isChecked = !item.isChecked
                putPrefBoolean(PreferKey.importKeepName, item.isChecked)
            }
        }
        return false
    }

    private fun alertCustomGroup(item: MenuItem) {
        alert(R.string.diy_edit_source_group) {
            val alertBinding = DialogCustomGroupBinding.inflate(layoutInflater).apply {
                val groups = linkedSetOf<String>()
                appDb.bookSourceDao.allGroup.forEach { group ->
                    groups.addAll(group.splitNotBlank(AppPattern.splitGroupRegex))
                }
                textInputLayout.setHint(R.string.group_name)
                editView.setFilterValues(groups.toList())
                editView.dropDownHeight = 180.dpToPx()
            }
            customView {
                alertBinding.root
            }
            okButton {
                viewModel.isAddGroup = alertBinding.swAddGroup.isChecked
                viewModel.groupName = alertBinding.editView.text?.toString()
                if (viewModel.groupName.isNullOrBlank()) {
                    item.title = getString(R.string.diy_source_group)
                } else {
                    val group = getString(R.string.diy_edit_source_group_title, viewModel.groupName)
                    if (viewModel.isAddGroup) {
                        item.title = "+$group"
                    } else {
                        item.title = group
                    }
                }
            }
            cancelButton()
        }
    }

    override fun onCodeSave(code: String, requestId: String?) {
        requestId?.toInt()?.let {
            BookSource.fromJson(code).getOrNull()?.let { source ->
                viewModel.allSources[it] = source
                adapter.setItem(it, source)
            }
        }
    }

    inner class SourcesAdapter(context: Context) :
        RecyclerAdapter<BookSource, ItemSourceImportBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemSourceImportBinding {
            return ItemSourceImportBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemSourceImportBinding,
            item: BookSource,
            payloads: MutableList<Any>
        ) {
            binding.apply {
                cbSourceName.isChecked = viewModel.selectStatus[holder.layoutPosition]
                cbSourceName.text = item.bookSourceName
                val localSource = viewModel.checkSources[holder.layoutPosition]
                tvSourceState.text = when {
                    localSource == null -> "新增"
                    item.lastUpdateTime > localSource.lastUpdateTime -> "更新"
                    else -> "已有"
                }
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemSourceImportBinding) {
            binding.apply {
                cbSourceName.setOnCheckedChangeListener { buttonView, isChecked ->
                    if (buttonView.isPressed) {
                        viewModel.selectStatus[holder.layoutPosition] = isChecked
                    }
                }
                root.onClick {
                    cbSourceName.isChecked = !cbSourceName.isChecked
                    viewModel.selectStatus[holder.layoutPosition] = cbSourceName.isChecked
                }
                tvOpen.setOnClickListener {
                    val source = viewModel.allSources[holder.layoutPosition]
                    showDialogFragment(
                        CodeDialog(
                            GSON.toJson(source),
                            disableEdit = false,
                            requestId = holder.layoutPosition.toString()
                        )
                    )
                }
            }
        }

    }

}