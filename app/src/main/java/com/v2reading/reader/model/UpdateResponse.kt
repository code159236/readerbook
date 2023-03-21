package com.v2reading.reader.model

import com.google.gson.annotations.SerializedName


data class UpdateResponse(

    @field:SerializedName("force")
    val force: Int = 0,

    @field:SerializedName("ad")
    val ad: Int = 1,

    @field:SerializedName("clear")
    val clear: Int = 0,

    @field:SerializedName("versionName")
    val versionName: String = "",

    @field:SerializedName("updateContent")
    val updateContent: String = "",

    @field:SerializedName("updateTitle")
    val updateTitle: String = "",

    @field:SerializedName("versionCode")
    val versionCode: Int = 0,

    @field:SerializedName("forceVersionCode")
    val forceVersionCode: Int = 0,

    @field:SerializedName("url")
    val url: String = "https://v2reading.com/download",

    @field:SerializedName("httpTTSVersion")
    val httpTTSVersion: Int = 0,

    @field:SerializedName("jhVersion")
    val jhVersion: Int = 0,

)
