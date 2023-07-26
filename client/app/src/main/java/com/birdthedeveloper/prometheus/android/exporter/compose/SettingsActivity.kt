// Author: Martin Ptacek

package com.birdthedeveloper.prometheus.android.exporter.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@Composable
fun SettingsPage(
    navController: NavHostController,
) {
    Column(
        Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(text = "Settings")
            },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        )

        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            Button(
                onClick = { navController.navigate("license") },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color.Gray)
            ) {
                Text(text = "License")
            }
        }
    }
}

@Composable
fun LicensePage(
    navController: NavHostController,
) {
    Column(
        Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text(text = "License")
            },
            navigationIcon = {
                IconButton(onClick = { navController.navigateUp() }) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                    )
                }
            }
        )
        Text(
            //TODO license
            modifier = Modifier.padding(all = 8.dp),
            text = "License todo"
        )
    }
}
