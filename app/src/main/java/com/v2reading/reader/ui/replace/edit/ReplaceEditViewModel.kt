package com.v2reading.reader.ui.replace.edit

import android.app.Application
import android.content.Intent
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.ReplaceRule

class ReplaceEditViewModel(application: Application) : BaseViewModel(application) {

    var replaceRule: ReplaceRule? = null

    fun initData(intent: Intent, finally: (replaceRule: ReplaceRule) -> Unit) {
        execute {
            val id = intent.getLongExtra("id", -1)
            if (id > 0) {
                replaceRule = appDb.replaceRuleDao.findById(id)
            } else {
                val pattern = intent.getStringExtra("pattern") ?: ""
                val isRegex = intent.getBooleanExtra("isRegex", false)
                val scope = intent.getStringExtra("scope")
                replaceRule = ReplaceRule(
                    name = pattern,
                    pattern = pattern,
                    isRegex = isRegex,
                    scope = scope
                )
            }
        }.onFinally {
            replaceRule?.let {
                finally(it)
            }
        }
    }

    fun save(replaceRule: ReplaceRule, success: () -> Unit) {
        execute {
            if (replaceRule.order == 0) {
                replaceRule.order = appDb.replaceRuleDao.maxOrder + 1
            }
            appDb.replaceRuleDao.insert(replaceRule)
        }.onSuccess {
            success()
        }
    }

}
