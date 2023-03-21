package com.v2reading.reader.ui.about

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseDialogFragment
import com.v2reading.reader.databinding.DialogUpdateBinding
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.model.Download
import com.v2reading.reader.utils.setLayout
import com.v2reading.reader.utils.toastOnUi
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
class UpdateDialog() : BaseDialogFragment(R.layout.dialog_update) {

    constructor(newVersion: String, updateBody: String, url: String, name: String) : this() {
        arguments = Bundle().apply {
            putString("newVersion", newVersion)
            putString("updateBody", updateBody)
            putString("url", url)
            putString("name", name)
        }
    }

    val binding by viewBinding(DialogUpdateBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.title = arguments?.getString("newVersion")
        val updateBody = arguments?.getString("updateBody")
        if (updateBody == null) {
            toastOnUi("没有数据")
            dismiss()
            return
        }
        if (!AppConfig.isGooglePlay) {
            binding.toolBar.inflateMenu(R.menu.app_update)
            binding.toolBar.setOnMenuItemClickListener {
                when (it.itemId) {
                    R.id.menu_download -> {
                        val url = arguments?.getString("url")
                        val name = arguments?.getString("name")
                        if (url != null && name != null) {
                            Download.start(requireContext(), url, name)
                        }
                    }
                }
                return@setOnMenuItemClickListener true
            }
        }
    }

}