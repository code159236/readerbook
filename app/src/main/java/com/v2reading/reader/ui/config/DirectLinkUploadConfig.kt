package com.v2reading.reader.ui.config

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseDialogFragment
import com.v2reading.reader.databinding.DialogDirectLinkUploadConfigBinding
import com.v2reading.reader.help.DirectLinkUpload
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.utils.setLayout
import com.v2reading.reader.utils.toastOnUi
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import splitties.views.onClick

class DirectLinkUploadConfig : BaseDialogFragment(R.layout.dialog_direct_link_upload_config) {

    private val binding by viewBinding(DialogDirectLinkUploadConfigBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.editUploadUrl.setText(DirectLinkUpload.getUploadUrl())
        binding.editDownloadUrlRule.setText(DirectLinkUpload.getDownloadUrlRule())
        binding.editSummary.setText(DirectLinkUpload.getSummary())
        binding.tvCancel.onClick {
            dismiss()
        }
        binding.tvFooterLeft.onClick {
            DirectLinkUpload.delete()
            dismiss()
        }
        binding.tvOk.onClick {
            val uploadUrl = binding.editUploadUrl.text?.toString()
            val downloadUrlRule = binding.editDownloadUrlRule.text?.toString()
            val summary = binding.editSummary.text?.toString()
            if (uploadUrl.isNullOrBlank()) {
                toastOnUi("上传Url不能为空")
                return@onClick
            }
            if (downloadUrlRule.isNullOrBlank()) {
                toastOnUi("下载Url规则不能为空")
                return@onClick
            }
            DirectLinkUpload.putUploadUrl(uploadUrl)
            DirectLinkUpload.putDownloadUrlRule(downloadUrlRule)
            DirectLinkUpload.putSummary(summary)
            dismiss()
        }
    }

}