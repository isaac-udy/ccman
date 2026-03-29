package feature.agent.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.enro.annotations.NavigationDestination
import feature.agent.domain.AgentOutput
import feature.agent.domain.AgentStatus
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@NavigationDestination(AgentDetailDestination::class)
fun AgentDetailScreen(viewModel: AgentDetailViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.output.size) {
        if (state.output.isNotEmpty()) {
            listState.animateScrollToItem(state.output.size - 1)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.agent?.name ?: "Agent") },
                navigationIcon = {
                    IconButton(onClick = viewModel::onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::onEditAgent) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit Agent")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = when (state.status) {
                        is AgentStatus.Idle -> "Idle"
                        is AgentStatus.Running -> "Running"
                        is AgentStatus.Error -> "Error: ${(state.status as AgentStatus.Error).message}"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    color = when (state.status) {
                        is AgentStatus.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
                        is AgentStatus.Running -> MaterialTheme.colorScheme.primary
                        is AgentStatus.Error -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.weight(1f),
                )
                FilterChip(
                    selected = state.showFullOutput,
                    onClick = viewModel::onToggleOutputDetail,
                    label = { Text(if (state.showFullOutput) "Full" else "Summary") },
                )
            }

            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp),
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                val visibleOutput = if (state.showFullOutput) {
                    state.output
                } else {
                    state.output.filterIsInstance<AgentOutput.Text>()
                }
                items(visibleOutput) { output ->
                    OutputItem(output)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = state.currentPrompt,
                    onValueChange = viewModel::onPromptChanged,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Enter a task...") },
                    enabled = state.status !is AgentStatus.Running,
                    singleLine = false,
                    maxLines = 3,
                )
                if (state.status is AgentStatus.Running) {
                    IconButton(onClick = viewModel::onStopTask) {
                        Icon(Icons.Default.Stop, contentDescription = "Stop")
                    }
                } else {
                    IconButton(
                        onClick = viewModel::onSendTask,
                        enabled = state.currentPrompt.isNotBlank(),
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Send")
                    }
                }
            }
        }
    }
}

@Composable
private fun OutputItem(output: AgentOutput) {
    when (output) {
        is AgentOutput.Text -> {
            Text(
                text = output.content,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
        is AgentOutput.ToolUse -> {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Tool: ${output.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = output.input,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
        is AgentOutput.ToolResult -> {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Result: ${output.name}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                    Text(
                        text = output.output,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                    )
                }
            }
        }
    }
}
