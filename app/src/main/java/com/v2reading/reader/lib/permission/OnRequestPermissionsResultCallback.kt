package com.v2reading.reader.lib.permission

interface OnRequestPermissionsResultCallback {

    fun onRequestPermissionsResult(permissions: Array<String>, grantResults: IntArray)

    fun onSettingActivityResult()
}
