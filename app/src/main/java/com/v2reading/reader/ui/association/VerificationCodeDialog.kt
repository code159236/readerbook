package com.v2reading.reader.ui.association

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseDialogFragment
import com.v2reading.reader.databinding.DialogVerificationCodeViewBinding
import com.v2reading.reader.help.CacheManager
import com.v2reading.reader.help.SourceVerificationHelp
import com.v2reading.reader.help.glide.ImageLoader
import com.v2reading.reader.help.glide.OkHttpModelLoader
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.ui.widget.dialog.PhotoDialog
import com.v2reading.reader.utils.*
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding

/**
 * 图片验证码对话框
 * 结果保存在内存中
 * val key = "${sourceOrigin ?: ""}_verificationResult"
 * CacheManager.get(key)
 */
class VerificationCodeDialog() : BaseDialogFragment(R.layout.dialog_verification_code_view), Toolbar.OnMenuItemClickListener {

    constructor(imageUrl: String, sourceOrigin: String? = null) : this() {
        arguments = Bundle().apply {
            putString("sourceOrigin", sourceOrigin)
            putString("imageUrl", imageUrl)
        }
    }

    val binding by viewBinding(DialogVerificationCodeViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    @SuppressLint("CheckResult")
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        initMenu()
        binding.run {
            toolBar.setBackgroundColor(primaryColor)
            val sourceOrigin = arguments?.getString("sourceOrigin")
            arguments?.getString("imageUrl")?.let { imageUrl ->
                ImageLoader.load(requireContext(), imageUrl).apply {
                    sourceOrigin?.let {
                        apply(
                            RequestOptions().set(
                                OkHttpModelLoader.sourceOriginOption,
                                it
                            )
                        )
                    }
                }.error(R.drawable.image_loading_error)
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .skipMemoryCache(true)
                    .into(verificationCodeImageView)
                verificationCodeImageView.setOnClickListener {
                    showDialogFragment(PhotoDialog(imageUrl, sourceOrigin))
                }
            }
        }
    }

    private fun initMenu() {
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.toolBar.inflateMenu(R.menu.verification_code)
        binding.toolBar.menu.applyTint(requireContext())
    }

    @SuppressLint("InflateParams")
    override fun onMenuItemClick(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_ok -> {
                val sourceOrigin = arguments?.getString("sourceOrigin")
                val key = "${sourceOrigin}_verificationResult"
                val verificationCode = binding.verificationCode.text.toString()
                verificationCode.let {
                    CacheManager.putMemory(key, it)
                    dismiss()
                }
           }
        }
        return false
    }

    override fun onDestroy() {
        SourceVerificationHelp.checkResult()
        super.onDestroy()
        activity?.finish()
    }

}