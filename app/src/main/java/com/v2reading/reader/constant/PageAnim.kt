package com.v2reading.reader.constant

import androidx.annotation.IntDef

object PageAnim {

    const val simulationPageAnim = 0

    const val slidePageAnim = 1

    const val coverPageAnim = 2

    const val scrollPageAnim = 3

    const val noAnim = 4

    @Target(AnnotationTarget.VALUE_PARAMETER)
    @Retention(AnnotationRetention.SOURCE)
    @IntDef(coverPageAnim, slidePageAnim, simulationPageAnim, scrollPageAnim, noAnim)
    annotation class Anim

}