package com.v2reading.reader.ui.font

import android.net.Uri
import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.LinearLayoutManager
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseDialogFragment
import com.v2reading.reader.constant.PreferKey
import com.v2reading.reader.databinding.DialogFontSelectBinding
import com.v2reading.reader.help.config.AppConfig
import com.v2reading.reader.lib.dialogs.SelectItem
import com.v2reading.reader.lib.dialogs.alert
import com.v2reading.reader.lib.permission.Permissions
import com.v2reading.reader.lib.permission.PermissionsCompat
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.ui.document.HandleFileContract
import com.v2reading.reader.utils.*
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.launch
import java.io.File

class FontSelectDialog : BaseDialogFragment(R.layout.dialog_font_select),
    Toolbar.OnMenuItemClickListener,
    FontAdapter.CallBack {
    private val fontRegex = Regex("(?i).*\\.[ot]tf")
    private val binding by viewBinding(DialogFontSelectBinding::bind)
    private val adapter by lazy {
        val curFontPath = callBack?.curFontPath ?: ""
        FontAdapter(requireContext(), curFontPath, this)
    }
    private val selectFontDir = registerForActivityResult(HandleFileContract()) {
        it.uri?.let { uri ->
            if (uri.toString().isContentScheme()) {
                putPrefString(PreferKey.fontFolder, uri.toString())
                val doc = DocumentFile.fromTreeUri(requireContext(), uri)
                if (doc != null) {
                    loadFontFiles(doc)
                } else {
                    RealPathUtil.getPath(requireContext(), uri)?.let { path ->
                        loadFontFilesByPermission(path)
                    }
                }
            } else {
                uri.path?.let { path ->
                    putPrefString(PreferKey.fontFolder, path)
                    loadFontFilesByPermission(path)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        setLayout(0.9f, 0.9f)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.setTitle(R.string.select_font)
        binding.toolBar.inflateMenu(R.menu.font_select)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener(this)
        binding.recyclerView.layoutManager = LinearLayoutManager(context)
        binding.recyclerView.adapter = adapter

        val fontPath = getPrefString(PreferKey.fontFolder)
        if (fontPath.isNullOrEmpty()) {
            openFolder()
        } else {
            if (fontPath.isContentScheme()) {
                val doc = DocumentFile.fromTreeUri(requireContext(), Uri.parse(fontPath))
                if (doc?.canRead() == true) {
                    loadFontFiles(doc)
                } else {
                    openFolder()
                }
            } else {
                loadFontFilesByPermission(fontPath)
            }
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {
            R.id.menu_default -> {
                val requireContext = requireContext()
                alert(titleResource = R.string.system_typeface) {
                    items(
                        requireContext.resources.getStringArray(R.array.system_typefaces).toList()
                    ) { _, i ->
                        AppConfig.systemTypefaces = i
                        onDefaultFontChange()
                        dismissAllowingStateLoss()
                    }
                }
            }
            R.id.menu_other -> {
                openFolder()
            }
        }
        return true
    }

    private fun openFolder() {
        launch(Main) {
            val defaultPath = "SD${File.separator}Fonts"
            selectFontDir.launch {
                otherActions = arrayListOf(SelectItem(defaultPath, -1))
            }
        }
    }

    private fun getLocalFonts(): ArrayList<FileDoc> {
        val path = FileUtils.getPath(requireContext().externalFiles, "font")
        return DocumentUtils.listFiles(path) {
            it.name.matches(fontRegex)
        }
    }

    private fun loadFontFiles(doc: DocumentFile) {
        execute {
            val fontItems = DocumentUtils.listFiles(doc.uri) {
                it.name.matches(fontRegex)
            }
            mergeFontItems(fontItems, getLocalFonts())
        }.onSuccess {
            adapter.setItems(it)
        }.onError {
            toastOnUi("getFontFiles:${it.localizedMessage}")
        }
    }

    private fun loadFontFilesByPermission(path: String) {
        PermissionsCompat.Builder(this@FontSelectDialog)
            .addPermissions(*Permissions.Group.STORAGE)
            .rationale(R.string.tip_perm_request_storage)
            .onGranted {
                loadFontFiles(path)
            }
            .request()
    }

    private fun loadFontFiles(path: String) {
        execute {
            val fontItems = DocumentUtils.listFiles(path) {
                it.name.matches(fontRegex)
            }
            mergeFontItems(fontItems, getLocalFonts())
        }.onSuccess {
            adapter.setItems(it)
        }.onError {
            toastOnUi("getFontFiles:${it.localizedMessage}")
        }
    }

    private fun mergeFontItems(
        items1: ArrayList<FileDoc>,
        items2: ArrayList<FileDoc>
    ): List<FileDoc> {
        val items = ArrayList(items1)
        items2.forEach { item2 ->
            var isInFirst = false
            items1.forEach for1@{ item1 ->
                if (item2.name == item1.name) {
                    isInFirst = true
                    return@for1
                }
            }
            if (!isInFirst) {
                items.add(item2)
            }
        }
        return items.sortedWith { o1, o2 ->
            o1.name.cnCompare(o2.name)
        }
    }

    override fun onFontSelect(docItem: FileDoc) {
        execute {
            callBack?.selectFont(docItem.toString())
        }.onSuccess {
            dismissAllowingStateLoss()
        }
    }

    private fun onDefaultFontChange() {
        callBack?.selectFont("")
    }

    private val callBack: CallBack?
        get() = (parentFragment as? CallBack) ?: (activity as? CallBack)

    interface CallBack {
        fun selectFont(path: String)
        val curFontPath: String
    }
}