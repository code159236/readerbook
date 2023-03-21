package com.v2reading.reader.ui.login

import android.content.DialogInterface
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.view.ViewGroup
import androidx.core.view.setPadding
import androidx.fragment.app.activityViewModels
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseDialogFragment
import com.v2reading.reader.constant.AppLog
import com.v2reading.reader.data.entities.BaseSource
import com.v2reading.reader.databinding.DialogLoginBinding
import com.v2reading.reader.databinding.ItemFilletTextBinding
import com.v2reading.reader.databinding.ItemSourceEditBinding
import com.v2reading.reader.lib.dialogs.alert
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.ui.about.AppLogDialog
import com.v2reading.reader.utils.*
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import splitties.views.onClick


class SourceLoginDialog : BaseDialogFragment(R.layout.dialog_login) {

    private val binding by viewBinding(DialogLoginBinding::bind)
    private val viewModel by activityViewModels<SourceLoginViewModel>()

    override fun onStart() {
        super.onStart()
        setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        val source = viewModel.source ?: return
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.title = getString(R.string.login_source, source.getTag())
        val loginInfo = source.getLoginInfoMap()
        val loginUi = source.loginUi()
        loginUi?.forEachIndexed { index, rowUi ->
            when (rowUi.type) {
                "text" -> ItemSourceEditBinding.inflate(layoutInflater, binding.root, false).let {
                    binding.flexbox.addView(it.root)
                    it.root.id = index
                    it.textInputLayout.hint = rowUi.name
                    it.editText.setText(loginInfo?.get(rowUi.name))
                }
                "password" -> ItemSourceEditBinding.inflate(layoutInflater, binding.root, false)
                    .let {
                        binding.flexbox.addView(it.root)
                        it.root.id = index
                        it.textInputLayout.hint = rowUi.name
                        it.editText.inputType =
                            InputType.TYPE_TEXT_VARIATION_PASSWORD or InputType.TYPE_CLASS_TEXT
                        it.editText.setText(loginInfo?.get(rowUi.name))
                    }
                "button" -> ItemFilletTextBinding.inflate(layoutInflater, binding.root, false).let {
                    binding.flexbox.addView(it.root)
                    it.root.id = index
                    it.textView.text = rowUi.name
                    it.textView.setPadding(16.dpToPx())
                    it.root.onClick {
                        if (rowUi.action.isAbsUrl()) {
                            context?.openUrl(rowUi.action!!)
                        }
                    }
                }
            }
        }
        binding.toolBar.inflateMenu(R.menu.source_login)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_ok -> {
                    val loginData = hashMapOf<String, String>()
                    loginUi?.forEachIndexed { index, rowUi ->
                        when (rowUi.type) {
                            "text", "password" -> {
                                val rowView = binding.root.findViewById<View>(index)
                                ItemSourceEditBinding.bind(rowView).editText.text?.let {
                                    loginData[rowUi.name] = it.toString()
                                }
                            }
                        }
                    }
                    login(source, loginData)
                }
                R.id.menu_show_login_header -> alert {
                    setTitle(R.string.login_header)
                    source.getLoginHeader()?.let { loginHeader ->
                        setMessage(loginHeader)
                    }
                }
                R.id.menu_del_login_header -> source.removeLoginHeader()
                R.id.menu_log -> showDialogFragment<AppLogDialog>()
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun login(source: BaseSource, loginData: HashMap<String, String>) {
        launch(IO) {
            if (loginData.isEmpty()) {
                source.removeLoginInfo()
                withContext(Main) {
                    dismiss()
                }
            } else if (source.putLoginInfo(GSON.toJson(loginData))) {
                source.getLoginJs()?.let {
                    try {
                        source.evalJS(it)
                        context?.toastOnUi(R.string.success)
                        withContext(Main) {
                            dismiss()
                        }
                    } catch (e: Exception) {
                        AppLog.put("登录出错\n${e.localizedMessage}", e)
                        context?.toastOnUi("登录出错\n${e.localizedMessage}")
                        e.printOnDebug()
                    }
                }
            }
        }
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        activity?.finish()
    }

}