package com.v2reading.reader.ui.book.read.config

import android.content.SharedPreferences
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.DialogFragment
import androidx.preference.ListPreference
import androidx.preference.Preference
import com.v2reading.reader.R
import com.v2reading.reader.constant.EventBus
import com.v2reading.reader.constant.PreferKey
import com.v2reading.reader.data.appDb
import com.v2reading.reader.help.IntentHelp
import com.v2reading.reader.lib.dialogs.SelectItem
import com.v2reading.reader.lib.prefs.fragment.PreferenceFragment
import com.v2reading.reader.lib.theme.backgroundColor
import com.v2reading.reader.lib.theme.primaryColor
import com.v2reading.reader.model.ReadAloud
import com.v2reading.reader.service.BaseReadAloudService
import com.v2reading.reader.utils.*

class ReadAloudConfigDialog : DialogFragment() {
    private val readAloudPreferTag = "readAloudPreferTag"

    override fun onStart() {
        super.onStart()
        dialog?.window?.run {
            setBackgroundDrawableResource(R.color.transparent)
            setLayout(0.9f, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = LinearLayout(requireContext())
        view.setBackgroundColor(requireContext().backgroundColor)
        view.id = R.id.tag1
        container?.addView(view)
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        var preferenceFragment = childFragmentManager.findFragmentByTag(readAloudPreferTag)
        if (preferenceFragment == null) preferenceFragment = ReadAloudPreferenceFragment()
        childFragmentManager.beginTransaction()
            .replace(view.id, preferenceFragment, readAloudPreferTag)
            .commit()
    }

    class ReadAloudPreferenceFragment : PreferenceFragment(),
        SpeakEngineDialog.CallBack,
        SharedPreferences.OnSharedPreferenceChangeListener {


        private val speakEngineSummary: String
            get() {
                val ttsEngine = ReadAloud.ttsEngine
                    ?: return getString(R.string.system_tts)
                if (StringUtils.isNumeric(ttsEngine)) {
                    return appDb.httpTTSDao.getName(ttsEngine.toLong())
                        ?: getString(R.string.system_tts)
                }
                return GSON.fromJsonObject<SelectItem<String>>(ttsEngine).getOrNull()?.title
                    ?: getString(R.string.system_tts)
            }

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.pref_config_aloud)
            upSpeakEngineSummary()
        }

        override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
            super.onViewCreated(view, savedInstanceState)
            listView.setEdgeEffectColor(primaryColor)
        }

        override fun onResume() {
            super.onResume()
            preferenceManager.sharedPreferences?.registerOnSharedPreferenceChangeListener(this)
        }

        override fun onPause() {
            preferenceManager.sharedPreferences?.unregisterOnSharedPreferenceChangeListener(this)
            super.onPause()
        }

        override fun onPreferenceTreeClick(preference: Preference): Boolean {
            when (preference.key) {
                PreferKey.ttsEngine -> showDialogFragment(SpeakEngineDialog(this))
                "sysTtsConfig" -> IntentHelp.openTTSSetting()
            }
            return super.onPreferenceTreeClick(preference)
        }


        override fun onSharedPreferenceChanged(
            sharedPreferences: SharedPreferences?,
            key: String?
        ) {
            when (key) {
                PreferKey.readAloudByPage -> {
                    if (BaseReadAloudService.isRun) {
                        postEvent(EventBus.MEDIA_BUTTON, false)
                    }
                }
            }
        }

        private fun upPreferenceSummary(preference: Preference?, value: String) {
            when (preference) {
                is ListPreference -> {
                    val index = preference.findIndexOfValue(value)
                    preference.summary = if (index >= 0) preference.entries[index] else null
                }
                else -> {
                    preference?.summary = value
                }
            }
        }

        override fun upSpeakEngineSummary() {
            upPreferenceSummary(
                findPreference(PreferKey.ttsEngine),
                speakEngineSummary
            )
        }
    }
}