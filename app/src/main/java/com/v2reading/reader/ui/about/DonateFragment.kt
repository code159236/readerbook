package com.v2reading.reader.ui.about

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import com.v2reading.reader.R
import com.v2reading.reader.utils.ACache
import com.v2reading.reader.utils.longToastOnUi
import com.v2reading.reader.utils.printOnDebug
import com.v2reading.reader.utils.sendToClip


class DonateFragment : PreferenceFragmentCompat() {


    override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
        addPreferencesFromResource(R.xml.donate)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        listView.overScrollMode = View.OVER_SCROLL_NEVER
    }

    override fun onPreferenceTreeClick(preference: Preference): Boolean {
        when (preference.key) {

        }
        return super.onPreferenceTreeClick(preference)
    }

    private fun getZfbHb(context: Context) {
        requireContext().sendToClip("537954522")
        context.longToastOnUi("高级功能已开启\n红包码已复制\n支付宝首页搜索“537954522” 立即领红包")
        try {
            val packageManager = context.applicationContext.packageManager
            val intent = packageManager.getLaunchIntentForPackage("com.eg.android.AlipayGphone")!!
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printOnDebug()
        } finally {
            ACache.get(requireContext(), cacheDir = false)
                .put("proTime", System.currentTimeMillis())
        }
    }

}