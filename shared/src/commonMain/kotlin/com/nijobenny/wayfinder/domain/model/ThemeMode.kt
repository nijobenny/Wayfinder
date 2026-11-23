package com.nijobenny.wayfinder.domain.model

/** User's appearance choice. [System] follows the device setting. */
enum class ThemeMode {
    System, Light, Dark;

    /** Cycle order used by the theme button: System → Light → Dark → System. */
    fun next(): ThemeMode = when (this) {
        System -> Light
        Light -> Dark
        Dark -> System
    }
}
