package feature.agent.ui.slack

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.enro.annotations.NavigationDestination
import feature.agent.domain.SlackConnectionStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@NavigationDestination(SlackDetailDestination::class)
fun SlackDetailScreen(viewModel: SlackDetailViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Slack Connection Details") },
                navigationIcon = {
                    IconButton(onClick = viewModel::onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(vertical = 8.dp),
            ) {
                Surface(
                    modifier = Modifier.size(12.dp),
                    shape = CircleShape,
                    color = when (state.connectionStatus) {
                        is SlackConnectionStatus.Connected -> Color(0xFF4CAF50)
                        is SlackConnectionStatus.Error -> Color(0xFFF44336)
                        else -> Color.Gray
                    },
                    content = {},
                )
                Text(
                    text = when (state.connectionStatus) {
                        is SlackConnectionStatus.Disconnected -> "Disconnected"
                        is SlackConnectionStatus.Connecting -> "Connecting..."
                        is SlackConnectionStatus.Connected -> "Connected"
                        is SlackConnectionStatus.Error -> "Error: ${(state.connectionStatus as SlackConnectionStatus.Error).message}"
                    },
                    style = MaterialTheme.typography.titleSmall,
                )
            }

            Text(
                text = "Messages Received (${state.messages.size})",
                style = MaterialTheme.typography.titleSmall,
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                items(state.messages.reversed()) { message ->
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
