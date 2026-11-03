package de.batteryng.mvp.measurement

import java.util.Locale
import kotlin.math.abs

/** Unit used to display a net battery current; readings remain in mA internally. */
enum class CurrentUnit(val symbol: String) {
    MILLIAMPS("mA"),
    AMPS("A"),
}

/** Formats the same measurement consistently on the main screen and in the overlay. */
object CurrentUnitFormatter {
    fun format(
        milliAmps: Double,
        unit: CurrentUnit,
        locale: Locale = Locale.getDefault(),
    ): String = when (unit) {
        CurrentUnit.MILLIAMPS -> {
            val pattern = if (abs(milliAmps) < 10.0) "%+.1f mA" else "%+.0f mA"
            if (abs(milliAmps) < 0.05) String.format(locale, "%.1f mA", 0.0)
            else String.format(locale, pattern, milliAmps)
        }
        CurrentUnit.AMPS -> {
            if (abs(milliAmps) < 0.5) String.format(locale, "%.3f A", 0.0)
            else String.format(locale, "%+.3f A", milliAmps / 1_000.0)
        }
    }

    fun unavailable(unit: CurrentUnit): String = "— ${unit.symbol}"
}
