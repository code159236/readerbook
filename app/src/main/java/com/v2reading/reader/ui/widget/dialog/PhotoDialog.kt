package com.v2reading.reader.ui.widget.dialog

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View
import com.bumptech.glide.request.RequestOptions
import com.v2reading.reader.R
import com.v2reading.reader.base.BaseDialogFragment
import com.v2reading.reader.databinding.DialogPhotoViewBinding
import com.v2reading.reader.help.BookHelp
import com.v2reading.reader.help.glide.ImageLoader
import com.v2reading.reader.help.glide.OkHttpModelLoader
import com.v2reading.reader.model.BookCover
import com.v2reading.reader.model.ReadBook
import com.v2reading.reader.utils.setLayout
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding

/**
 * 显示图片
 */
class PhotoDialog() : BaseDialogFragment(R.layout.dialog_photo_view) {

    constructor(src: String, sourceOrigin: String? = null) : this() {
        arguments = Bundle().apply {
            putString("src", src)
            putString("sourceOrigin", sourceOrigin)
        }
    }

    private val binding by viewBinding(DialogPhotoViewBinding::bind)

    override fun onStart() {
        super.onStart()
        setLayout(1f, 1f)
    }

    @SuppressLint("CheckResult")
    override fun onFragmentCreated(view: View, savedInstanceState: Bundle?) {
        arguments?.let { arguments ->
            arguments.getString("src")?.let { src ->
                val file = ReadBook.book?.let { book ->
                    BookHelp.getImage(book, src)
                }
                if (file?.exists() == true) {
                    ImageLoader.load(requireContext(), file)
                        .error(R.drawable.image_loading_error)
                        .into(binding.photoView)
                } else {
                    ImageLoader.load(requireContext(), src).apply {
                        arguments.getString("sourceOrigin")?.let { sourceOrigin ->
                            apply(
                                RequestOptions().set(
                                    OkHttpModelLoader.sourceOriginOption,
                                    sourceOrigin
                                )
                            )
                        }
                    }.error(BookCover.defaultDrawable)
                        .into(binding.photoView)
                }
            }
        }

    }

}
