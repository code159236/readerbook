package com.v2reading.reader.ui.config

import android.os.Bundle
import androidx.activity.viewModels
import androidx.fragment.app.Fragment
import com.v2reading.reader.R
import com.v2reading.reader.base.VMBaseActivity
import com.v2reading.reader.constant.EventBus
import com.v2reading.reader.databinding.ActivityConfigBinding
import com.v2reading.reader.utils.observeEvent
import com.v2reading.reader.utils.viewbindingdelegate.viewBinding

class ConfigActivity : VMBaseActivity<ActivityConfigBinding, ConfigViewModel>() {

    override val binding by viewBinding(ActivityConfigBinding::inflate)
    override val viewModel by viewModels<ConfigViewModel>()

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        when (val configTag = intent.getStringExtra("configTag")) {
            ConfigTag.OTHER_CONFIG -> replaceFragment<OtherConfigFragment>(configTag)
            ConfigTag.THEME_CONFIG -> replaceFragment<ThemeConfigFragment>(configTag)
            ConfigTag.BACKUP_CONFIG -> replaceFragment<BackupConfigFragment>(configTag)
            ConfigTag.COVER_CONFIG -> replaceFragment<CoverConfigFragment>(configTag)
            ConfigTag.WELCOME_CONFIG -> replaceFragment<WelcomeConfigFragment>(configTag)
            else -> finish()
        }
    }

    override fun setTitle(resId: Int) {
        super.setTitle(resId)
        binding.titleBar.setTitle(resId)
    }

    inline fun <reified T : Fragment> replaceFragment(configTag: String) {
        intent.putExtra("configTag", configTag)
        val configFragment = supportFragmentManager.findFragmentByTag(configTag)
            ?: T::class.java.newInstance()
        supportFragmentManager.beginTransaction()
            .replace(R.id.configFrameLayout, configFragment, configTag)
            .commit()
    }

    override fun observeLiveBus() {
        super.observeLiveBus()
        observeEvent<String>(EventBus.RECREATE) {
            recreate()
        }
    }

    override fun finish() {
        if (supportFragmentManager.findFragmentByTag(ConfigTag.COVER_CONFIG) != null
            || supportFragmentManager.findFragmentByTag(ConfigTag.WELCOME_CONFIG) != null
        ) {
            replaceFragment<ThemeConfigFragment>(ConfigTag.THEME_CONFIG)
        } else {
            super.finish()
        }
    }

}