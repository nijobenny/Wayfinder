package com.nijobenny.wayfinder

import androidx.compose.ui.window.ComposeUIViewController
import com.nijobenny.wayfinder.di.initKoin
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    initKoin()
    return ComposeUIViewController { App() }
}
