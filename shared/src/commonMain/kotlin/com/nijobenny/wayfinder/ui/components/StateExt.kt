package com.nijobenny.wayfinder.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.StateFlow

/** Short alias for lifecycle-aware collection. */
@Composable
fun <T> StateFlow<T>.collectAsStateLifecycle(): State<T> = collectAsStateWithLifecycle()
