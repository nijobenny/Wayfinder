package com.nijobenny.wayfinder.platform

import androidx.compose.runtime.Composable

/** Current time in epoch milliseconds. */
expect fun currentTimeMillis(): Long

/** Absolute path of the DataStore preferences file for this platform. */
expect fun dataStoreFilePath(): String

/** Makes status/navigation bar icons dark on light themes and light on dark themes. */
@Composable
expect fun SystemBarsEffect(useDarkIcons: Boolean)
