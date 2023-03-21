package com.v2cross.app

import android.content.Context

object Intro {

    external fun getToken(context: Context?, userId: String?): String?

    external fun checkSha1(context: Context?): Boolean

    init {
        System.loadLibrary("VerificationLib")
    }
}