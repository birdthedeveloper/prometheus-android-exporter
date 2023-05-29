package com.birdthedeveloper.prometheus.android.prometheus.android.exporter.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
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
    promViewModel: PromViewModel,
    navController: NavHostController,
) {
    val uiState : PromUiState by promViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ){
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

        // depending on whether the configuration file is present
//        when(uiState.configFileState){
//            ConfigFileState.ERROR -> ConfigFileErrorPage(promViewModel = promViewModel)
//            ConfigFileState.SUCCESS -> ConfigFileSuccessPage(promViewModel = promViewModel)
//            ConfigFileState.LOADING -> LoadingPage()
//            ConfigFileState.MISSING -> TabPage(promViewModel, navController)
//        }
        Column(
            modifier = Modifier.fillMaxHeight().weight(1f)
        ) {
            Text("something")
        }
        
        Button(
            onClick = { print("TODO") },
            modifier = Modifier
                .height(40.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Start")
        }
    }

}

@Composable
private fun TabPage(
    promViewModel: PromViewModel,
    navController: NavHostController,
){
    val tabs = mapOf(0 to "Server", 1 to "PushProx")
    val uiState : PromUiState by promViewModel.uiState.collectAsState()

    Column(modifier = Modifier.fillMaxWidth()) {
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
            2 -> RemoteWritePage(promViewModel)
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
) {
    if (value) {
        val result: String? = promViewModel.turnPushProxOn()
        if (result != null) {
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

    // if showDialogText is empty string, do not display alert dialog
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

        if(uiState.pushProxTurnedOn){
            Text(
                text = """
                    To edit PushProx proxy URL or FQDN, turn it off first.
                """.trimIndent(),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 12.dp),
            )
        }

        TextField(
            value = uiState.fqdn,
            singleLine = true,
            enabled = !uiState.pushProxTurnedOn,
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
            singleLine = true,
            enabled = !uiState.pushProxTurnedOn,
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

@Composable
private fun RemoteWritePage(
    viewModel: PromViewModel
){

}

@Composable
private fun LoadingPage(){
    Column (
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxHeight()
    ) {
        Spacer(modifier = Modifier.height(50.dp))
        Text(text = "Checking for configuration file")
        Spacer(modifier = Modifier.height(20.dp))
        CircularProgressIndicator(modifier = Modifier.size(size = 36.dp))
    }
}

@Composable
private fun ConfigFileErrorPage(
    promViewModel: PromViewModel,
){
    val uiState : PromUiState by promViewModel.uiState.collectAsState()

    //TODO implement this
    Text("Config File error page")
}

@Composable
private fun ConfigFileSuccessPage(
    promViewModel: PromViewModel,
){
    val uiState : PromUiState by promViewModel.uiState.collectAsState()

    //TODO implement this
    Text("config file success page")
}

