package com.v2reading.reader.lib.webdav

class WebDavException(msg: String) : Exception(msg) {

    override fun fillInStackTrace(): Throwable {
        return this
    }

}