package com.location

import android.app.Activity
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod

class SplashModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private var themeChanged = false // Flag to track if the theme has been changed

    override fun getName(): String {
        return "SplashModule" // This is the name of the module to be used in JavaScript
    }

    @ReactMethod
    fun hide() {
        // Ensure that the method is called on the main thread
        val currentActivity = currentActivity as? MainActivity
        currentActivity?.runOnUiThread {
            if (!themeChanged) {
                currentActivity.setTheme(R.style.AppTheme) // Change to your main app theme
                themeChanged = true // Set the flag to true
                currentActivity.recreate() // Recreate the activity to apply the new theme
            }
        }
    }
}
