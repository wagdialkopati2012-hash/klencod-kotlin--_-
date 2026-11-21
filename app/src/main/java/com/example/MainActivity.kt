package com.example

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.TenderingAppRoot
import com.example.ui.theme.TenderingTheme
import com.example.ui.viewmodel.MainViewModel
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        configureOptimalRefreshRate()

        setContent {
            val themeSettings by mainViewModel.themeSettings.collectAsStateWithLifecycle()
            val sessionState by mainViewModel.sessionState.collectAsStateWithLifecycle()
            val companyInfo by mainViewModel.companyInfo.collectAsStateWithLifecycle()

            TenderingTheme(settings = themeSettings) {
                TenderingAppRoot(
                    sessionState = sessionState,
                    companyInfo = companyInfo,
                    onLogout = mainViewModel::logout
                )
            }
        }
    }

    /**
     * Selects the highest refresh rate mode natively supported by the display
     * at its current physical resolution (e.g. 120Hz / 90Hz / 60Hz) without
     * forcing unsupported modes.
     */
    private fun configureOptimalRefreshRate() {
        val currentDisplay = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            display
        } else {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay
        } ?: return

        val activeMode = currentDisplay.mode
        val bestMode = currentDisplay.supportedModes
            .filter {
                it.physicalWidth == activeMode.physicalWidth &&
                    it.physicalHeight == activeMode.physicalHeight
            }
            .maxByOrNull { it.refreshRate }

        if (bestMode != null && bestMode.modeId != activeMode.modeId) {
            val params = window.attributes
            params.preferredDisplayModeId = bestMode.modeId
            window.attributes = params
        }
    }
}