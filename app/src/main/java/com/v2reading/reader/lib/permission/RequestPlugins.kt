package com.v2reading.reader.lib.permission

internal object RequestPlugins {

    @Volatile
    var sRequestCallback: OnRequestPermissionsResultCallback? = null

    @Volatile
    var sResultCallback: OnPermissionsResultCallback? = null

    fun setOnRequestPermissionsCallback(callback: OnRequestPermissionsResultCallback) {
        sRequestCallback = callback
    }

    fun setOnPermissionsResultCallback(callback: OnPermissionsResultCallback) {
        sResultCallback = callback
    }


}
