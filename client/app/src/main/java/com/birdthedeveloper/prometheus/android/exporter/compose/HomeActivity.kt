// Author: Martin Ptacek

package com.birdthedeveloper.prometheus.android.exporter.compose

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Switch
import androidx.compose.material.Tab
import androidx.compose.material.TabRow
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.TextField
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

private const val TAG: String = "HOMEPAGE"

@Composable
fun HomePage(
    promViewModel: PromViewModel,
    navController: NavHostController,
) {
    val uiState: PromUiState by promViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        TopAppBar(
            title = {
                Text("Prometheus Android Exporter")
            },
            actions = {
                IconButton(
                    onClick = {
                        navController.navigate("settings") {
                            launchSingleTop = true
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings"
                    )
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)
        ) {
            // depending on whether the configuration file is present
            when (uiState.configFileState) {
                ConfigFileState.ERROR -> ConfigFileErrorPage(
                    promViewModel = promViewModel,
                    Modifier
                )

                ConfigFileState.SUCCESS -> ConfigFileSuccessPage(promViewModel = promViewModel)
                ConfigFileState.LOADING -> LoadingPage(Modifier)
                ConfigFileState.MISSING -> TabPage(promViewModel)
            }
        }

        StartStopButton(promViewModel)

        if (uiState.configValidationException != null) {
            ValidationExceptionDialog(promViewModel = promViewModel)
        }
    }
}

@Composable
private fun ValidationExceptionDialog(
    promViewModel: PromViewModel
) {
    val uiState: PromUiState by promViewModel.uiState.collectAsState()

    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(text = "Configuration not valid")
        },
        text = {
            Text(text = uiState.configValidationException ?: "")
        },
        confirmButton = {},
        dismissButton = {
            TextButton(
                onClick = {
                    promViewModel.dismissValidationExceptionDialog()
                }
            ) {
                Text(text = "Dismiss")
            }
        }
    )
}

@Composable
private fun StartStopButton(
    promViewModel: PromViewModel
) {
    val uiState: PromUiState by promViewModel.uiState.collectAsState()
    val fileState: ConfigFileState = uiState.configFileState

    val redColor = Color(117, 8, 8, 255)
    val greenColor = Color(0, 105, 16, 255)
    val orangeColor = Color(186, 96, 6)

    if (fileState == ConfigFileState.SUCCESS || fileState == ConfigFileState.MISSING) {
        val buttonColor: Color
        val buttonText: String

        when (uiState.exporterState) {
            ExporterState.Running -> {
                buttonText = "STOP"
                buttonColor = redColor
            }

            ExporterState.NotRunning -> {
                buttonText = "START"
                buttonColor = greenColor
            }

            ExporterState.Enqueued -> {
                buttonText = "STOP (work is enqueued)"
                buttonColor = orangeColor
            }
        }


        Button(
            onClick = { promViewModel.toggleIsRunning() },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = buttonColor, contentColor = Color.White,
            ),
            shape = RoundedCornerShape(0),
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
        ) {
            Text(text = buttonText)
        }
    }
}

@Composable
private fun TabPage(
    promViewModel: PromViewModel,
) {
    val tabs = mapOf(0 to "Prom Server", 1 to "PushProx", 2 to "Batch exporter")
    val uiState: PromUiState by promViewModel.uiState.collectAsState()

    Column {
        TabRow(selectedTabIndex = uiState.tabIndex) {
            tabs.forEach { (index, text) ->
                Tab(text = { Text(text) },
                    selected = index == uiState.tabIndex,
                    onClick = { promViewModel.updateTabIndex(index) })
            }
        }
        when (uiState.tabIndex) {
            0 -> PrometheusServerPage(promViewModel)
            1 -> PushProxPage(promViewModel)
            2 -> RemoteWritePage(promViewModel)
        }
    }
}

@Composable
private fun PrometheusServerPage(
    promViewModel: PromViewModel,
) {
    val uiState: PromUiState by promViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        TextField(
            value = uiState.promConfig.prometheusServerPort,
            singleLine = true,
            onValueChange = {
                promViewModel.updatePromConfig(UpdatePromConfig.PrometheusServerPort, it)
            },
            label = { Text("Prometheus HTTP port") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            enabled = uiState.exporterState == ExporterState.NotRunning,
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Turn on Android Exporter"
        )
        Switch(
            checked = uiState.promConfig.prometheusServerEnabled,
            onCheckedChange = { value: Boolean? ->
                if (value != null) {
                    promViewModel.updatePromConfig(UpdatePromConfig.PrometheusServerEnabled, value)
                }
            },
            enabled = uiState.exporterState == ExporterState.NotRunning,
        )
    }
}

@Composable
private fun PushProxPage(
    promViewModel: PromViewModel,
) {
    val uiState: PromUiState by promViewModel.uiState.collectAsState()

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
            value = uiState.promConfig.pushproxFqdn,
            singleLine = true,
            onValueChange = {
                promViewModel.updatePromConfig(UpdatePromConfig.PushproxFqdn, it)
            },
            label = {
                Text(text = "Fully Qualified Domain Name")
            },
            modifier = Modifier.padding(bottom = 12.dp),
            enabled = uiState.exporterState == ExporterState.NotRunning,
        )

        TextField(
            value = uiState.promConfig.pushproxProxyUrl,
            singleLine = true,
            onValueChange = {
                promViewModel.updatePromConfig(UpdatePromConfig.PushproxProxyUrl, it)
            },
            label = {
                Text(text = "PushProx proxy URL")
            },
            enabled = uiState.exporterState == ExporterState.NotRunning,
        )

        Switch(
            checked = uiState.promConfig.pushproxEnabled,
            onCheckedChange = { value: Boolean? ->
                if (value != null) {
                    promViewModel.updatePromConfig(UpdatePromConfig.PushproxEnabled, value)
                }
            },
            enabled = uiState.exporterState == ExporterState.NotRunning,
        )

    }
}

@Composable
private fun RemoteWritePage(
    promViewModel: PromViewModel,
) {
    val uiState: PromUiState by promViewModel.uiState.collectAsState()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Remote write configuration:")

        Spacer(modifier = Modifier.padding(bottom = 12.dp))

        TextField(
            value = uiState.promConfig.remoteWriteEndpoint,
            singleLine = true,
            onValueChange = {
                promViewModel.updatePromConfig(UpdatePromConfig.RemoteWriteEndpoint, it)
            },
            label = {
                Text(text = "Remote write endpoint")
            },
            modifier = Modifier.padding(bottom = 12.dp),
            enabled = uiState.exporterState == ExporterState.NotRunning,
        )

        TextField(
            value = uiState.promConfig.remoteWriteScrapeInterval,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            onValueChange = {
                promViewModel.updatePromConfig(UpdatePromConfig.RemoteWriteScrapeInterval, it)
            },
            label = {
                Text(text = "Scrape interval in seconds")
            },
            enabled = uiState.exporterState == ExporterState.NotRunning,
        )

        TextField(
            value = uiState.promConfig.remoteWriteInstanceLabel,
            singleLine = true,
            onValueChange = {
                promViewModel.updatePromConfig(UpdatePromConfig.RemoteWriteInstanceLabel, it)
            },
            label = {
                Text(text = "Target label instance")
            },
            enabled = uiState.exporterState == ExporterState.NotRunning,
        )

        TextField(
            value = uiState.promConfig.remoteWriteJobLabel,
            singleLine = true,
            onValueChange = {
                promViewModel.updatePromConfig(UpdatePromConfig.RemoteWriteJobLabel, it)
            },
            label = {
                Text(text = "Target label job")
            },
            enabled = uiState.exporterState == ExporterState.NotRunning,
        )

        Switch(
            checked = uiState.promConfig.remoteWriteEnabled,
            onCheckedChange = { value: Boolean? ->
                if (value != null) {
                    promViewModel.updatePromConfig(UpdatePromConfig.RemoteWriteEnabled, value)
                }
            },
            enabled = uiState.exporterState == ExporterState.NotRunning,
        )
    }
}

@Composable
private fun LoadingPage(
    modifier: Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier,
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
    modifier: Modifier,
) {
    val uiState: PromUiState by promViewModel.uiState.collectAsState()

    Column(
        modifier = modifier,
    ) {
        Text("Config File error:", modifier = Modifier.padding(vertical = 20.dp))
        if (uiState.fileLoadException != null) {
            Text(uiState.fileLoadException!!)
        }
    }
}

@Composable
private fun ConfigFileSuccessPage(
    promViewModel: PromViewModel,
) {
    val uiState: PromUiState by promViewModel.uiState.collectAsState()

    Column {
        Text("Config file success:", modifier = Modifier.padding(vertical = 20.dp))
        Text(uiState.promConfig.toStructuredText())
    }
}
