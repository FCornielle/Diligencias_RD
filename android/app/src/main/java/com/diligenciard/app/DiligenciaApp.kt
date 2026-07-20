package com.diligenciard.app

import android.app.Application
import com.diligenciard.app.util.RuntimeMode
import com.google.android.libraries.places.api.Places

class DiligenciaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (RuntimeMode.googleCloudEnabled && !Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(this, BuildConfig.MAPS_API_KEY)
        }
    }
}
