package com.v2reading.reader.ui.book.read.config

import android.app.Application
import com.v2reading.reader.base.BaseViewModel
import com.v2reading.reader.data.appDb
import com.v2reading.reader.data.entities.TxtTocRule
import com.v2reading.reader.help.DefaultData

class TocRegexViewModel(application: Application) : BaseViewModel(application) {

    fun saveRule(rule: TxtTocRule) {
        execute {
            if (rule.serialNumber < 0) {
                rule.serialNumber = appDb.txtTocRuleDao.maxOrder + 1
            }
            appDb.txtTocRuleDao.insert(rule)
        }
    }

    fun importDefault() {
        execute {
            DefaultData.importDefaultTocRules()
        }
    }

}