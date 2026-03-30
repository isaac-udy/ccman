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
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.enro.annotations.NavigationDestination
import feature.agent.domain.AgentOutput
import feature.agent.domain.AgentStatus
import feature.agent.domain.AgentTask
import kotlin.time.Instant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@NavigationDestination(AgentDetailDestination::class)
fun AgentDetailScreen(viewModel: AgentDetailViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(state.currentTask?.output?.size) {
        val totalItems = listState.layoutInfo.totalItemsCount
        if (totalItems > 0) {
            listState.animateScrollToItem(totalItems - 1)
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
                        is AgentStatus.Running -> "Running - ${formatDuration(state.elapsedSeconds)}"
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

            SelectionContainer(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    // Task history
                    if (state.taskHistory.isNotEmpty()) {
                        item(key = "history-header") {
                            Text(
                                text = "Task History",
                                style = MaterialTheme.typography.titleSmall,
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                        items(state.taskHistory, key = { "history-${it.id.value}" }) { task ->
                            TaskHistoryCard(task = task, showFullOutput = state.showFullOutput)
                        }
                        item(key = "history-divider") {
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                        }
                    }

                    // Current task
                    val currentTask = state.currentTask
                    if (currentTask != null) {
                        item(key = "current-header") {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    text = "Running (${formatDuration(state.elapsedSeconds)})",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                        }
                        item(key = "current-prompt") {
                            PromptCard(prompt = currentTask.prompt)
                        }
                        val output = if (state.showFullOutput) {
                            currentTask.output
                        } else {
                            currentTask.output.filter { it is AgentOutput.Text || it is AgentOutput.Result }
                        }
                        items(output.size, key = { "current-output-$it" }) { index ->
                            OutputItem(output[index], showRawJson = state.showFullOutput)
                        }
                    }

                    // Input row
                    item(key = "input") {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
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
                                maxLines = 5,
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
        }
    }
}

@Composable
private fun PromptCard(prompt: String) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Prompt",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = prompt,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        }
    }
}

@Composable
private fun TaskHistoryCard(task: AgentTask, showFullOutput: Boolean) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = buildString {
                        append(
                            when (task.status) {
                                AgentTask.Status.Completed -> "Completed"
                                AgentTask.Status.Error -> "Error"
                                AgentTask.Status.Running -> "Running"
                            }
                        )
                        val duration = taskDuration(task)
                        if (duration != null) {
                            append(" (${formatDuration(duration)})")
                        }
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (task.status) {
                        AgentTask.Status.Completed -> MaterialTheme.colorScheme.primary
                        AgentTask.Status.Error -> MaterialTheme.colorScheme.error
                        AgentTask.Status.Running -> MaterialTheme.colorScheme.tertiary
                    },
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = task.startedAt.substringBefore("T").let { date ->
                        val time = task.startedAt.substringAfter("T").substringBefore(".").take(5)
                        "$date $time"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = task.prompt,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(8.dp),
                    maxLines = if (showFullOutput) Int.MAX_VALUE else 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (showFullOutput) {
                task.output.forEach { output ->
                    OutputItem(output, showRawJson = true)
                }
            } else {
                val resultText = task.output
                    .filterIsInstance<AgentOutput.Text>()
                    .lastOrNull()
                    ?: task.output.filterIsInstance<AgentOutput.Result>().lastOrNull()
                if (resultText != null) {
                    val content = when (resultText) {
                        is AgentOutput.Text -> resultText.content
                        is AgentOutput.Result -> resultText.content
                        else -> ""
                    }
                    if (content.isNotBlank()) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            AgentOutputMarkdown(
                                content = content,
                                modifier = Modifier.padding(8.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OutputItem(output: AgentOutput, showRawJson: Boolean) {
    Column {
        OutputItemContent(output)
        if (showRawJson) {
            Text(
                text = output.rawJson,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
            )
        }
    }
}

@Composable
private fun OutputItemContent(output: AgentOutput) {
    when (output) {
        is AgentOutput.Text -> {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AgentOutputMarkdown(
                    content = output.content,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
        is AgentOutput.Thinking -> {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Thinking",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = output.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
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
                        text = "Tool",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = output.name + if (output.description != null) " - ${output.description}" else "",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = output.command,
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
        is AgentOutput.Result -> {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                AgentOutputMarkdown(
                    content = output.content,
                    modifier = Modifier.padding(8.dp),
                )
            }
        }
        is AgentOutput.Unknown -> {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(modifier = Modifier.padding(8.dp)) {
                    Text(
                        text = "Unknown type: ${output.type}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Text(
                        text = output.rawJson,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }
            }
        }
    }
}

private fun formatDuration(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}

private fun taskDuration(task: AgentTask): Long? {
    val startedAt = try {
        Instant.parse(task.startedAt)
    } catch (_: Throwable) {
        return null
    }
    val completedAt = try {
        task.completedAt?.let { Instant.parse(it) }
    } catch (_: Throwable) {
        null
    } ?: return null
    return (completedAt - startedAt).inWholeSeconds
}
