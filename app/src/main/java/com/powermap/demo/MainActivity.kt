package com.powermap.demo

import android.Manifest
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import com.powermap.demo.repository.CustomPoiRepository
import com.powermap.sdk.PowerMapSDK
import com.powermap.sdk.ui.PowerMapView
import com.powermap.demo.ui.MainMapScreen

/**
 * Demo App MainActivity — Demo-Driven approach.
 * Each button tests one SDK method to verify it works before building out the full UI.
 *
 * Mirrors the Flutter powermapp_mobile_sdk_demo app's feature coverage.
 */
class MainActivity : ComponentActivity() {

    private var powerMapView: PowerMapView? = null

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        PowerMapSDK.initialize(
            mapApiKey    = BuildConfig.MAP_API_KEY,
            clientId     = BuildConfig.CLIENT_ID,
            clientSecret = BuildConfig.CLIENT_SECRET
        )
        locationPermissionRequest.launch(
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        )

        val poiRepository = CustomPoiRepository(applicationContext)
        poiRepository.load()

        setContent {
            com.powermap.demo.theme.AppTheme {
                MainMapScreen(
                    poiRepository = poiRepository,
                    onViewCreated = { view -> powerMapView = view }
                )
            }
        }
    }

    override fun onStart() { super.onStart(); powerMapView?.onStart() }
    override fun onResume() { super.onResume(); powerMapView?.onResume() }
    override fun onPause() { super.onPause(); powerMapView?.onPause() }
    override fun onStop() { super.onStop(); powerMapView?.onStop() }
    override fun onDestroy() { 
        super.onDestroy()
        powerMapView?.onDestroy()
        
        // Ensure background navigation service stops when app is closed
        val intent = android.content.Intent(this, com.powermap.sdk.navigation.LocationTrackingService::class.java)
        intent.action = com.powermap.sdk.navigation.LocationTrackingService.ACTION_STOP
        startService(intent)
    }
}
