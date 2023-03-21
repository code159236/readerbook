package com.v2reading.reader.utils

import android.content.Context
import androidx.core.content.edit
import com.v2reading.reader.constant.AppConst
import com.v2reading.reader.constant.PreferKey
import java.util.*

/**
 * Created by crosserr on 2021/8/25.
 */
object CoinUtil {

    fun addTodayCoins(date: String, coins: Int, context: Context) {

        val tag = date + "coins"

        val currentCoins = context.defaultSharedPreferences.getInt(tag, 0)

        val totalCoins = context.defaultSharedPreferences.getInt(PreferKey.totalCoin, 0)

        context.defaultSharedPreferences.edit {
            putInt(tag, currentCoins + coins)
            putInt(PreferKey.totalCoin, totalCoins + coins)
        }


    }


    fun reduceCoins(coins: Int, context: Context): Boolean {

        val tag = PreferKey.totalCoin

        val currentCoins = context.defaultSharedPreferences.getInt(tag, 0)

        val i = currentCoins - coins
        if (i < 0) {
            return false
        }
        context.defaultSharedPreferences.edit { putInt(tag, i) }
        return true
    }


    fun getCoins(context: Context): Int {

        val tag = PreferKey.totalCoin
        return context.defaultSharedPreferences.getInt(tag, 0)
    }

    fun getTodayCoins(context: Context): Int {
        val tag = AppConst.dateOnlyFormat.format(Date()) + "coins"
        return context.defaultSharedPreferences.getInt(tag, 0)
    }


}