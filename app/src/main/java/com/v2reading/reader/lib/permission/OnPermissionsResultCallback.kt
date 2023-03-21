package com.v2reading.reader.lib.permission

interface OnPermissionsResultCallback {

    fun onPermissionsGranted()

    fun onPermissionsDenied(deniedPermissions: Array<String>)

}