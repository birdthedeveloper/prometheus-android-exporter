package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.compose

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.birdthedeveloper.prometheus.android.prometheus.android.exporter.ui.theme.PrometheusAndroidExporterTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val promViewModel: PromViewModel = ViewModelProvider(this)[PromViewModel::class.java]
        promViewModel.initializeWithApplicationContext { this.applicationContext }

        setContent {
            PrometheusAndroidExporterTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    PromNavigation(promViewModel = promViewModel)
                }
            }
        }
    }

    @Composable
    private fun PromNavigation(
        navController : NavHostController = rememberNavController(),
        promViewModel : PromViewModel
    ){
        val startDestination : String = "homepage"
        NavHost(
            navController = navController,
            startDestination = startDestination,
        ){
            composable("settings") {
                SettingsPage(
                    navController = navController,
                    promViewModel = promViewModel,
                )
            }
            composable("homepage") {
                HomePage(
                    navController = navController,
                    promViewModel = promViewModel
                )
            }
            composable("license") { LicensePage(navController = navController) }
        }
    }
}
