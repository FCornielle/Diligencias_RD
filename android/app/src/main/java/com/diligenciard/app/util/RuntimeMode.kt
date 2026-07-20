package com.diligenciard.app.util

import com.diligenciard.app.BuildConfig

object RuntimeMode {
    private const val PLACEHOLDER_KEY = "PLACEHOLDER_API_KEY"

    val googleCloudEnabled: Boolean
        get() = BuildConfig.MAPS_API_KEY.isNotBlank() &&
            BuildConfig.MAPS_API_KEY != PLACEHOLDER_KEY &&
            BuildConfig.MAPS_API_KEY.startsWith("AIza")
}
