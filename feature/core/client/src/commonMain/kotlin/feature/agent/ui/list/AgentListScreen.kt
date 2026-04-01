package feature.agent.ui.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
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
import feature.agent.domain.Agent
import feature.agent.domain.AgentStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@NavigationDestination(AgentListDestination::class)
fun AgentListScreen(viewModel: AgentListViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CCMan - Agents") },
                actions = {
                    IconButton(onClick = viewModel::onOpenSettings) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::onCreateAgent) {
                Icon(Icons.Default.Add, contentDescription = "Create Agent")
            }
        }
    ) { padding ->
        if (state.groups.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "No agents configured. Tap + to create one.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                state.groups.forEach { group ->
                    item(key = "group-header-${group.name}") {
                        Text(
                            text = group.name,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp),
                        )
                    }
                    items(group.agents, key = { it.agent.id.value }) { entry ->
                        AgentCard(
                            entry = entry,
                            onClick = { viewModel.onAgentSelected(entry.agent.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AgentCard(
    entry: AgentListState.AgentListEntry,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusIndicator(entry.status)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.agent.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = entry.agent.workingDirectory,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = when (entry.status) {
                    is AgentStatus.Idle -> "Idle"
                    is AgentStatus.Running -> "Running"
                    is AgentStatus.Error -> "Error"
                },
                style = MaterialTheme.typography.labelMedium,
                color = when (entry.status) {
                    is AgentStatus.Idle -> MaterialTheme.colorScheme.onSurfaceVariant
                    is AgentStatus.Running -> MaterialTheme.colorScheme.primary
                    is AgentStatus.Error -> MaterialTheme.colorScheme.error
                },
            )
        }
    }
}

@Composable
private fun StatusIndicator(status: AgentStatus) {
    val color = when (status) {
        is AgentStatus.Idle -> Color.Gray
        is AgentStatus.Running -> Color(0xFF4CAF50)
        is AgentStatus.Error -> Color(0xFFF44336)
    }
    Surface(
        modifier = Modifier.size(12.dp),
        shape = CircleShape,
        color = color,
        content = {},
    )
}
