package com.v2reading.reader.ui.widget

import android.content.Context
import android.view.ViewGroup
import android.widget.PopupWindow
import com.v2reading.reader.base.adapter.ItemViewHolder
import com.v2reading.reader.base.adapter.RecyclerAdapter
import com.v2reading.reader.databinding.ItemTextBinding
import com.v2reading.reader.databinding.PopupActionBinding
import com.v2reading.reader.lib.dialogs.SelectItem
import splitties.systemservices.layoutInflater

class PopupAction(private val context: Context) :
    PopupWindow(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT) {

    val binding = PopupActionBinding.inflate(context.layoutInflater)
    val adapter by lazy {
        Adapter(context)
    }
    var onActionClick: ((action: String) -> Unit)? = null

    init {
        contentView = binding.root

        isTouchable = true
        isOutsideTouchable = false
        isFocusable = true

        binding.recyclerView.adapter = adapter
    }

    fun setItems(items: List<SelectItem<String>>) {
        adapter.setItems(items)
    }

    inner class Adapter(context: Context) :
        RecyclerAdapter<SelectItem<String>, ItemTextBinding>(context) {

        override fun getViewBinding(parent: ViewGroup): ItemTextBinding {
            return ItemTextBinding.inflate(inflater, parent, false)
        }

        override fun convert(
            holder: ItemViewHolder,
            binding: ItemTextBinding,
            item: SelectItem<String>,
            payloads: MutableList<Any>
        ) {
            with(binding) {
                textView.text = item.title
            }
        }

        override fun registerListener(holder: ItemViewHolder, binding: ItemTextBinding) {
            holder.itemView.setOnClickListener {
                getItem(holder.layoutPosition)?.let { item ->
                    onActionClick?.invoke(item.value)
                }
            }
        }
    }

}