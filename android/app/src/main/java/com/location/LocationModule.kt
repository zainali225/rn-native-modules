package com.location

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule.RCTDeviceEventEmitter
import com.google.android.gms.location.*
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.tasks.Task

class LocationModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private var locationUpdateAllowed = true
    private val timeoutDuration = 5000L // Timeout of 5 seconds to avoid multiple updates
    private var locationPromise: Promise? = null
    private val REQUEST_CHECK_SETTINGS = 0x1

    override fun getName(): String {
        return "LocationModule"
    }

    @ReactMethod
    fun checkAndRequestLocation(promise: Promise) {
        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000).build()

        val builder = LocationSettingsRequest.Builder().addLocationRequest(locationRequest)
        val settingsClient = LocationServices.getSettingsClient(reactApplicationContext)
        val task: Task<LocationSettingsResponse> = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener { response ->
            // Location is enabled
            promise.resolve(true)
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    // Location settings are not satisfied, but this can be fixed by showing the user a dialog.
                    locationPromise = promise // Store the promise to resolve later
                    currentActivity?.let { exception.startResolutionForResult(it, REQUEST_CHECK_SETTINGS) }
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Failed to show dialog
                    promise.reject("LocationError", "Failed to ask user to enable location")
                }
            } else {
                // Cannot resolve location settings issue
                promise.reject("LocationDisabled", "Location services are disabled and cannot be resolved")
            }
        }
    }

    // Handle the result of the dialog
    fun onActivityResult(activity: Activity?, requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (locationPromise != null) {
                if (resultCode == Activity.RESULT_OK) {
                    // User enabled location
                    locationPromise?.resolve(true)
                } else {
                    // User denied enabling location
                    locationPromise?.resolve(false)
                }
                locationPromise = null // Clear the promise after handling it
            }
        }
    }


    @SuppressLint("MissingPermission")
    @ReactMethod
    fun startLocationUpdates() {

        Log.d("LocationModule", "Starting Location")
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(reactApplicationContext)

        val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000) // 10 seconds interval
            .setMinUpdateIntervalMillis(10000)  // Minimum time between updates
            .setMaxUpdateDelayMillis(20000)     // Maximum delay for update
            .build()
        Log.d("LocationModule", "$locationRequest")

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                Log.d("LocationModule", "LocationResult : Lat = ${locationResult},")

                val location = locationResult.lastLocation

                if (location != null && locationUpdateAllowed) {
                    locationUpdateAllowed = false // Disable further updates temporarily
                    val locationMap = Arguments.createMap()
                    locationMap.putDouble("latitude", location.latitude)
                    locationMap.putDouble("longitude", location.longitude)

                    Log.d("LocationModule", "Location Update: Lat = ${location.latitude}, Lon = ${location.longitude}")

                    // Send location data to React Native
                    sendLocationToReactNative(locationMap)

                    // Start a timeout of 5 seconds before allowing another update
                    Handler(Looper.getMainLooper()).postDelayed({
                        locationUpdateAllowed = true
                    }, timeoutDuration)
                }
            }

            override fun onLocationAvailability(locationAvailability: LocationAvailability) {
                Log.d("Location Module", "locationAvailability is " + locationAvailability.isLocationAvailable)
                super.onLocationAvailability(locationAvailability)
            }
        }

        // Request location updates
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        Log.d("LocationModule", "Requested location updates from FusedLocationProvider")
    }


    @ReactMethod
    fun stopLocationUpdates() {
        if (::fusedLocationClient.isInitialized && ::locationCallback.isInitialized) {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }

    private fun sendLocationToReactNative(locationMap: WritableMap) {
        reactApplicationContext
            .getJSModule(RCTDeviceEventEmitter::class.java)
            .emit("locationUpdated", locationMap)
    }

    private fun sendErrorToReactNative(errorCode: String, errorMessage: String) {
        val errorMap = Arguments.createMap()
        errorMap.putString("errorCode", errorCode)
        errorMap.putString("errorMessage", errorMessage)
        reactApplicationContext
            .getJSModule(RCTDeviceEventEmitter::class.java)
            .emit("locationError", errorMap)
    }
}
