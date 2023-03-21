package com.v2reading.reader.ui.book.read

import android.app.Application
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseDialogFragment
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.BookChapter
import com.v2reading.reader.databinding.DialogContentEditBinding
import com.v2reading.reader.databinding.DialogEditTextBinding
import com.v2reading.reader.help.BookHelp
import com.v2reading.reader.help.ContentProcessor
import com.v2reading.reader.lib.dialogs.alert
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.model.ReadBook
import com.v2reading.reader.model.webBook.WebBook
import com.v2reading.reader.utils.applyTint
import com.v2reading.reader.utils.sendToClip
import com.v2reading.reader.utils.setLayout
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 内容编辑
 */
class ContentEditDialog : BaseDialogFragment(R.layout.dialog_content_edit) {

    val binding by viewBinding(DialogContentEditBinding::bind)
    val viewModel by viewModels<ContentEditViewModel>()

    override fun onStart() {
        super.onStart()
        setLayout(1f, ViewGroup.LayoutParams.MATCH_PARENT)
    }

    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        binding.toolBar.setBackgroundColor(primaryColor)
        binding.toolBar.title = ReadBook.curTextChapter?.title
        initMenu()
        binding.toolBar.setOnClickListener {
            launch {
                val book = ReadBook.book ?: return@launch
                val chapter = withContext(IO) {
                    appDb.bookChapterDao.getChapter(book.bookUrl, ReadBook.durChapterIndex)
                } ?: return@launch
                editTitle(chapter)
            }
        }
        viewModel.initContent {
            binding.contentView.setText(it)
            binding.contentView.post {
                binding.contentView.apply {
                    val lineIndex = layout.getLineForOffset(ReadBook.durChapterPos)
                    val lineHeight = layout.getLineTop(lineIndex)
                    scrollTo(0, lineHeight)
                }
            }
        }
    }

    private fun initMenu() {
        binding.toolBar.inflateMenu(R.menu.content_edit)
        binding.toolBar.menu.applyTint(requireContext())
        binding.toolBar.setOnMenuItemClickListener {
            when (it.itemId) {
                R.id.menu_save -> launch {
                    binding.contentView.text?.toString()?.let { content ->
                        withContext(IO) {
                            val book = ReadBook.book ?: return@withContext
                            val chapter = appDb.bookChapterDao
                                .getChapter(book.bookUrl, ReadBook.durChapterIndex)
                                ?: return@withContext
                            BookHelp.saveText(book, chapter, content)
                        }
                    }
                    ReadBook.loadContent(ReadBook.durChapterIndex, resetPageOffset = false)
                    dismiss()
                }
                R.id.menu_reset -> viewModel.initContent(true) { content ->
                    binding.contentView.setText(content)
                    ReadBook.loadContent(ReadBook.durChapterIndex, resetPageOffset = false)
                }
                R.id.menu_copy_all -> requireContext()
                    .sendToClip("${binding.toolBar.title}\n${binding.contentView.text}")
            }
            return@setOnMenuItemClickListener true
        }
    }

    private fun editTitle(chapter: BookChapter) {
        alert {
            setTitle(R.string.edit)
            val alertBinding = DialogEditTextBinding.inflate(layoutInflater)
            alertBinding.editView.setText(chapter.title)
            setCustomView(alertBinding.root)
            okButton {
                chapter.title = alertBinding.editView.text.toString()
                launch {
                    withContext(IO) {
                        appDb.bookChapterDao.upDate(chapter)
                    }
                    binding.toolBar.title = chapter.getDisplayTitle()
                    ReadBook.loadContent(ReadBook.durChapterIndex, resetPageOffset = false)
                }
            }
        }
    }

    class ContentEditViewModel(application: Application) : BaseViewModel(application) {

        var content: String? = null

        fun initContent(reset: Boolean = false, success: (String) -> Unit) {
            execute {
                val book = ReadBook.book ?: return@execute null
                val chapter = appDb.bookChapterDao
                    .getChapter(book.bookUrl, ReadBook.durChapterIndex)
                    ?: return@execute null
                if (reset) {
                    content = null
                    BookHelp.delContent(book, chapter)
                    if (!book.isLocalBook()) ReadBook.bookSource?.let { bookSource ->
                        WebBook.getContentAwait(this, bookSource, book, chapter)
                    }
                }
                return@execute content ?: let {
                    val contentProcessor = ContentProcessor.get(book.name, book.origin)
                    val content = BookHelp.getContent(book, chapter) ?: return@let null
                    contentProcessor.getContent(book, chapter, content, includeTitle = false)
                        .joinToString("\n")
                }
            }.onSuccess {
                content = it
                success.invoke(it ?: "")
            }
        }

    }

}