package com.nijobenny.wayfinder

import android.content.Context
import com.nijobenny.wayfinder.di.initKoin
import com.nijobenny.wayfinder.platform.AndroidContextHolder

/** Call once from MainActivity before showing [App]. */
fun initWayfinder(context: Context) {
    AndroidContextHolder.init(context)
    initKoin()
}
