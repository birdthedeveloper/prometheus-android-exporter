package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController

@Composable
fun SettingsPage(
    promViewModel: PromViewModel,
    navController: NavHostController,
){
    Column(
        Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(text = "Settings")
            },
            navigationIcon = {
                IconButton(onClick = {navController.navigateUp() }){
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        )
    }
}