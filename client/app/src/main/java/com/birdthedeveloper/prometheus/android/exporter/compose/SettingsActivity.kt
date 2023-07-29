// Author: Martin Ptacek

package com.birdthedeveloper.prometheus.android.exporter.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
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

       Text( modifier = Modifier.padding(all = 20.dp),
           text = "This application is licensed under the Apache 2.0 license.", textAlign = TextAlign.Center,)
        Text(modifier = Modifier.padding(all = 20.dp),text = "Author: Martin Ptacek, 2023", textAlign = TextAlign.Center,)
    }
}
