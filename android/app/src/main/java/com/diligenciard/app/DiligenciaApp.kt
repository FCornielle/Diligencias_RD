package com.diligenciard.app

import android.app.Application
import com.diligenciard.app.util.RuntimeMode
import com.google.android.libraries.places.api.Places
import org.osmdroid.config.Configuration

class DiligenciaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Configuration.getInstance().userAgentValue = packageName
        if (RuntimeMode.googleCloudEnabled && !Places.isInitialized()) {
            Places.initializeWithNewPlacesApiEnabled(this, BuildConfig.MAPS_API_KEY)
        }
    }
}
