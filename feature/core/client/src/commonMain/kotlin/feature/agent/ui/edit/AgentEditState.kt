package feature.agent.ui.edit

data class AgentEditState(
    val isNew: Boolean = true,
    val name: String = "",
    val workingDirectory: String = "",
    val instructions: String = "",
    val showDeleteDialog: Boolean = false,
)
