package com.v2reading.reader.ui.book.read.config

import android.annotation.SuppressLint
import android.content.DialogInterface
import android.content.SharedPreferences
import android.os.Bundle
import android.view.*
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import androidx.preference.Preference
import com.v2reading.reader.R
import com.v2reading.reader.constant.EventBus
import com.v2reading.reader.constant.PreferKey
import com.v2reading.reader.help.config.ReadBookConfig
import com.v2reading.reader.lib.prefs.fragment.PreferenceFragment
import com.v2reading.reader.lib.theme.bottomBackground
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.model.ReadBook
import com.v2reading.reader.ui.book.read.ReadBookActivity
import com.v2reading.reader.ui.book.read.page.provider.ChapterProvider
import com.v2reading.reader.utils.dpToPx
import com.v2reading.reader.utils.getPrefBoolean
import com.v2reading.reader.utils.postEvent
import com.v2reading.reader.utils.setEdgeEffectColor

class MoreConfigDialog : DialogFragment() {
    private val readPreferTag = "readPreferenceFragment"

    override fun onStart() {
        super.onStart()
        dialog?.window?.run {
            clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
            setBackgroundDrawableResource(R.color.background)
            decorView.setPadding(0, 0, 0, 0)
            val attr = attributes
            attr.dimAmount = 0.0f
            attr.gravity = Gravity.BOTTOM
            attributes = attr
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, 360.dpToPx())
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        (activity as ReadBookActivity).bottomDialog++
        val view = LinearLayout(context)
        view.setBackgroundColor(requireContext().bottomBackground)
        view.id = R.id.tag1
        container?.addView(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var preferenceFragment = childFragmentManager.findFragmentByTag(readPreferTag)
        if (preferenceFragment == null) preferenceFragment = ReadPreferenceFragment()
        childFragmentManager.beginTransaction()
            .replace(view.id, preferenceFragment, readPreferTag)
            .commit()
    }

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        (activity as ReadBookActivity).bottomDialog--
    }

    class ReadPreferenceFragment : PreferenceFragment(),
        SharedPreferences.OnSharedPreferenceChangeListener {

        @SuppressLint("RestrictedApi")
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_config_read)
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.setEdgeEffectColor(primaryColor)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager
                .sharedPreferences
                ?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager
                .sharedPreferences
                ?.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
            when (key) {
                PreferKey.readBodyToLh -> activity?.recreate()
                PreferKey.hideStatusBar -> {
                    ReadBookConfig.hideStatusBar = getPrefBoolean(PreferKey.hideStatusBar)
                    postEvent(EventBus.UP_CONFIG, true)
                }
                PreferKey.hideNavigationBar -> {
                    ReadBookConfig.hideNavigationBar = getPrefBoolean(PreferKey.hideNavigationBar)
                    postEvent(EventBus.UP_CONFIG, true)
                }
                PreferKey.keepLight -> postEvent(key, true)
                PreferKey.textSelectAble -> postEvent(key, getPrefBoolean(key))
                PreferKey.screenOrientation -> {
                    (activity as? ReadBookActivity)?.setOrientation()
                }
                PreferKey.textFullJustify,
                PreferKey.textBottomJustify,
                PreferKey.useZhLayout -> {
                    postEvent(EventBus.UP_CONFIG, true)
                }
                PreferKey.showBrightnessView -> {
                    postEvent(PreferKey.showBrightnessView, "")
                }
                PreferKey.expandTextMenu -> {
                    (activity as? ReadBookActivity)?.textActionMenu?.upMenu()
                }
                PreferKey.doublePageHorizontal -> {
                    ChapterProvider.upLayout()
                    ReadBook.loadContent(false)
                }
            }
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                "customPageKey" -> PageKeyDialog(requireContext()).show()
                "clickRegionalConfig" -> {
                    (activity as? ReadBookActivity)?.showClickRegionalConfig()
                }
            }
            return super.onPreferenceTreeClick(preference)
        }

    }
}