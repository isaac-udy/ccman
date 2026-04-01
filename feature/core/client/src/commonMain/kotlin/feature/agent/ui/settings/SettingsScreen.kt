package feature.agent.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.enro.annotations.NavigationDestination
import feature.agent.domain.SlackConnectionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@NavigationDestination(SettingsDestination::class)
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = viewModel::onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Slack Integration",
                style = MaterialTheme.typography.titleMedium,
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Enable Slack",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Switch(
                            checked = state.slackEnabled,
                            onCheckedChange = viewModel::onSlackEnabledChanged,
                        )
                    }

                    OutlinedTextField(
                        value = state.slackBotToken,
                        onValueChange = viewModel::onSlackBotTokenChanged,
                        label = { Text("Bot Token (xoxb-...)") },
                        placeholder = { Text("xoxb-...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    OutlinedTextField(
                        value = state.slackAppToken,
                        onValueChange = viewModel::onSlackAppTokenChanged,
                        label = { Text("App Token (xapp-...)") },
                        placeholder = { Text("xapp-...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )

                    OutlinedButton(
                        onClick = viewModel::onSaveSlackConfig,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Save Configuration")
                    }
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Connection Status",
                        style = MaterialTheme.typography.titleSmall,
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = when (state.slackConnectionStatus) {
                                is SlackConnectionStatus.Disconnected -> "Disconnected"
                                is SlackConnectionStatus.Connecting -> "Connecting..."
                                is SlackConnectionStatus.Connected -> "Connected"
                                is SlackConnectionStatus.Error -> "Error: ${(state.slackConnectionStatus as SlackConnectionStatus.Error).message}"
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = when (state.slackConnectionStatus) {
                                is SlackConnectionStatus.Disconnected -> MaterialTheme.colorScheme.onSurfaceVariant
                                is SlackConnectionStatus.Connecting -> MaterialTheme.colorScheme.tertiary
                                is SlackConnectionStatus.Connected -> MaterialTheme.colorScheme.primary
                                is SlackConnectionStatus.Error -> MaterialTheme.colorScheme.error
                            },
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    val isConnected = state.slackConnectionStatus is SlackConnectionStatus.Connected
                    val hasTokens = state.slackBotToken.isNotBlank() && state.slackAppToken.isNotBlank()

                    if (isConnected) {
                        Button(
                            onClick = viewModel::onDisconnectSlack,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                        ) {
                            Text("Disconnect")
                        }
                    } else {
                        Button(
                            onClick = viewModel::onConnectSlack,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = hasTokens && !state.slackConnecting,
                        ) {
                            Text(if (state.slackConnecting) "Connecting..." else "Connect to Slack")
                        }
                    }

                    OutlinedButton(
                        onClick = viewModel::onSendTestMessage,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = isConnected,
                    ) {
                        Text("Send Test Message to #ccman-test")
                    }
                }
            }

            if (state.slackMessages.isNotEmpty()) {
                Text(
                    text = "Incoming Messages",
                    style = MaterialTheme.typography.titleMedium,
                )

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        state.slackMessages.reversed().forEach { message ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                                ),
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                    ) {
                                        Text(
                                            text = "#${message.channelName}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                        Text(
                                            text = message.timestamp
                                                .substringAfter("T")
                                                .substringBefore(".")
                                                .take(8),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Text(
                                        text = message.text,
                                        style = MaterialTheme.typography.bodySmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
