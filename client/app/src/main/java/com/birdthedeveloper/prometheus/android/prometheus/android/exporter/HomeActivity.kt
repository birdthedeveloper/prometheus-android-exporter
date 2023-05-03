package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.getValue
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@Composable
fun HomePage(
    modifier : Modifier = Modifier,
    promViewModel: PromViewModel = viewModel(),
    navController: NavHostController,
) {
    val tabs = mapOf(0 to "Server", 1 to "PushProx")
    val uiState : PromUiState by promViewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
        TopAppBar(
            title = {
                Text("Android Exporter")
            },
            actions = {
                IconButton(
                    onClick = {
                        navController.navigate("settings"){
                            launchSingleTop = true
                        }
                    }
                ){
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        )
        TabRow(selectedTabIndex = uiState.tabIndex) {
            tabs.forEach{ (index, text) ->
                Tab(text = {Text(text)},
                    selected = index == uiState.tabIndex,
                    onClick = { promViewModel.updateTabIndex(index) })
            }
        }
        when(uiState.tabIndex){
            0 -> ServerPage()
            1 -> PushProxPage()
        }
    }

    //TODO tabview
}

@Composable
private fun ServerPage(
    promViewModel: PromViewModel = viewModel()
){
    Text("Server page")
    //TODO implement this thing
}

@Composable
private fun PushProxPage(
    promViewModel: PromViewModel = viewModel()
){
    Text("PushProx config page")
    //TODO implement this
}