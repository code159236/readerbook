package com.v2reading.reader.ui.widget.dialog

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import com.hsalf.smileyrating.SmileyRating
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseDialogFragment
import com.v2reading.reader.databinding.DialogReviewViewBinding
import com.v2reading.reader.utils.setLayout
import com.v2reading.reader.utils.toastOnUi
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding


class RateDialog() : BaseDialogFragment(R.layout.dialog_review_view) {


    constructor(
        content: String?
    ) : this() {
    }

    private val binding by viewBinding(DialogReviewViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {

        binding.smileRating.setSmileySelectedListener {
            if (SmileyRating.Type.GREAT == it) {
                review()
            } else toastOnUi("评价成功,感谢您的评价")

            dismiss()
        }

    }

    private fun review() {
        try {
            val uri = Uri.parse("market://details?id=" + context?.packageName)
            val intent = Intent(Intent.ACTION_VIEW, uri)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
    }
}
