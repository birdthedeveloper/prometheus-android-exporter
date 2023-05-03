package com.birdthedeveloper.prometheus.android.prometheus.android.exporter

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Switch
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

private val TAG = "HOMEPAGE"

@Composable
fun HomePage(
    modifier : Modifier = Modifier,
    promViewModel: PromViewModel,
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
            0 -> ServerPage(promViewModel)
            1 -> PushProxPage(promViewModel)
        }
    }
}

private fun onCheckedChangeServer(
    value : Boolean,
    promViewModel: PromViewModel,
    showDialog : MutableState<String>
){
    if (value) {
        val result : String? = promViewModel.turnServerOn()
        if(result != null){
            showDialog.value = result
        }
    } else {
        promViewModel.turnServerOff()
    }
}

@Composable
private fun ServerPage(
    promViewModel: PromViewModel
){
    val uiState : PromUiState by promViewModel.uiState.collectAsState()

    // if showDialogText == "", do not display alert dialog
    val showDialogText : MutableState<String> = remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(text = "Turn on Android Exporter on default port ${promViewModel.getDefaultPort()}")
        Switch(
            checked = uiState.serverTurnedOn,
            onCheckedChange = {value : Boolean? ->
                if(value != null){
                    onCheckedChangeServer(value, promViewModel, showDialogText)
                }
            }
        )
        if(showDialogText.value != ""){
            AlertDialog(
                onDismissRequest = { showDialogText.value = "" },
                title = { Text ("Error") },
                text = { Text(showDialogText.value) },
                dismissButton = {
                    Button(
                        onClick = { showDialogText.value = "" }
                    ){ Text("OK") }
                },
                confirmButton = {}
            )
        }
    }
}

private fun onCheckedChangePushProx(
    value : Boolean,
    promViewModel: PromViewModel,
    showDialog : MutableState<String>
){
    if (value) {
        val result : String? = promViewModel.turnPushProxOn()
        if(result != null){
            showDialog.value = result
        }
    } else {
        promViewModel.turnPushProxOff()
    }
}

@Composable
private fun PushProxPage(
    promViewModel: PromViewModel
){
    val uiState : PromUiState by promViewModel.uiState.collectAsState()

    // if showDialogText == "", do not display alert dialog
    val showDialogText : MutableState<String> = remember { mutableStateOf("") }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = """
                Configuration of PushProx client for traversing NAT
                while still following the pull model.
            """.trimIndent(),
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(bottom = 30.dp)
        )

        TextField(
            value = uiState.fqdn,
            onValueChange = {
                promViewModel.updatePushProxFQDN(it)
            },
            label = {
                Text(text = "Fully Qualified Domain Name")
            },
            modifier = Modifier.padding(bottom = 12.dp)
        )

        TextField(
            value = uiState.pushProxURL,
            onValueChange = {
                promViewModel.updatePushProxURL(it)
            },
            label = {
                Text(text = "PushProx proxy URL")
            },
        )

        Switch(
            checked = uiState.pushProxTurnedOn,
            onCheckedChange = {value : Boolean? ->
                if(value != null){
                    onCheckedChangePushProx(value, promViewModel, showDialogText)
                }
            }
        )

        // conditional alert dialog
        if(showDialogText.value != ""){
            AlertDialog(
                onDismissRequest = { showDialogText.value = "" },
                title = { Text ("Error") },
                text = { Text(showDialogText.value) },
                dismissButton = {
                    Button(
                        onClick = { showDialogText.value = "" }
                    ){ Text("OK") }
                },
                confirmButton = {}
            )
        }
    }
}

//TODO asap: 4 screens: loading, file error, file loaded, file missing - already implemented

@Composable
private fun LoadingPage(){
    val progress : Float by remember { mutableStateOf(0.0f) }
    //TODO finish this thing
}
//TODO
//https://foso.github.io/Jetpack-Compose-Playground/material/circularprogressindicator/