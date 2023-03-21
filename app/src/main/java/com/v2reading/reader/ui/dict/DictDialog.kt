package com.v2reading.reader.ui.dict

import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseDialogFragment
import com.v2reading.reader.databinding.DialogDictBinding
import com.v2reading.reader.utils.invisible
import com.v2reading.reader.utils.setHtml
import com.v2reading.reader.utils.setLayout
import com.v2reading.reader.utils.toastOnUi
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding

/**
 * 词典
 */
class DictDialog() : BaseDialogFragment(R.layout.dialog_dict) {

    constructor(word: String) : this() {
        arguments = Bundle().apply {
            putString("word", word)
        }
    }

    private val viewModel by viewModels<DictViewModel>()
    private val binding by viewBinding(DialogDictBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.tvDict.movementMethod = LinkMovementMethod()
        val word = arguments?.getString("word")
        if (word.isNullOrEmpty()) {
            toastOnUi(R.string.cannot_empty)
            dismiss()
            return
        }
        viewModel.dictHtmlData.observe(viewLifecycleOwner) {
            binding.rotateLoading.invisible()
            binding.tvDict.setHtml(it)
        }
        viewModel.dict(word)

    }


}