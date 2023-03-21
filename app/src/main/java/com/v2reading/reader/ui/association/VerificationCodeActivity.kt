package com.v2reading.reader.ui.association

import android.os.Bundle
import com.v2reading.reader.base.BaseActivity
import com.v2reading.reader.databinding.ActivityTranslucenceBinding
import com.v2reading.reader.utils.showDialogFragment
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding

/**
 * 验证码
 */
class VerificationCodeActivity :
    BaseActivity<ActivityTranslucenceBinding>() {

    override val binding by viewBinding(ActivityTranslucenceBinding::inflate)

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        intent.getStringExtra("imageUrl")?.let {
            val sourceOrigin = intent.getStringExtra("sourceOrigin")
            showDialogFragment(
                VerificationCodeDialog(it, sourceOrigin)
            )
        } ?: finish()
    }

}