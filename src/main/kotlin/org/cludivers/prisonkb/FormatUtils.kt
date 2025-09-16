package org.cludivers.prisonkb

import java.util.*

object FormatUtils {
    fun format(v: Double): String = String.format(Locale.US, "%.2f", v)
}