package feature.agent.ui.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import dev.enro.annotations.NavigationDestination

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@NavigationDestination(AgentEditDestination::class)
fun AgentEditScreen(viewModel: AgentEditViewModel = viewModel()) {
    val state by viewModel.state.collectAsState()

    if (state.showDeleteDialog) {
        DeleteAgentDialog(
            workingDirectory = state.workingDirectory,
            onDismiss = viewModel::onDeleteDismissed,
            onConfirm = viewModel::onDeleteConfirmed,
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isNew) "Create Agent" else "Edit Agent") },
                navigationIcon = {
                    IconButton(onClick = viewModel::onCancel) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = state.name,
                onValueChange = viewModel::onNameChanged,
                label = { Text("Agent Name") },
                placeholder = { Text("e.g., my-project-agent-1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = state.workingDirectory,
                onValueChange = viewModel::onWorkingDirectoryChanged,
                label = { Text("Working Directory") },
                placeholder = { Text("e.g., /home/user/repos/my-project-1") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
            )

            OutlinedTextField(
                value = state.instructions,
                onValueChange = viewModel::onInstructionsChanged,
                label = { Text("Instructions (optional)") },
                placeholder = { Text("Instructions to include with every task sent to this agent...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                minLines = 3,
                maxLines = 8,
            )

            Spacer(modifier = Modifier.weight(1f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (!state.isNew) {
                    Button(
                        onClick = viewModel::onDeleteRequested,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError,
                        ),
                    ) {
                        Text("Delete")
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                OutlinedButton(onClick = viewModel::onCancel) {
                    Text("Cancel")
                }
                Button(
                    onClick = viewModel::onSave,
                    enabled = state.name.isNotBlank() && state.workingDirectory.isNotBlank(),
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun DeleteAgentDialog(
    workingDirectory: String,
    onDismiss: () -> Unit,
    onConfirm: (alsoDeleteDirectory: Boolean) -> Unit,
) {
    var deleteDirectory by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Agent") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Are you sure you want to delete this agent?")
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = deleteDirectory,
                        onCheckedChange = { deleteDirectory = it },
                    )
                    Column {
                        Text(
                            text = "Also delete directory on disk",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        Text(
                            text = workingDirectory,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(deleteDirectory) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                ),
            ) {
                Text(if (deleteDirectory) "Delete Agent & Directory" else "Delete Agent")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
    )
}
