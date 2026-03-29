package feature.agent.ui.edit

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.enro.close
import dev.enro.navigationHandle
import dev.isaacudy.udytils.state.ViewModelState
import dev.isaacudy.udytils.state.viewModelState
import feature.agent.domain.Agent
import feature.agent.domain.FlowOfAgents
import feature.agent.domain.SaveAgent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class AgentEditViewModel(
    private val flowOfAgents: FlowOfAgents,
    private val saveAgent: SaveAgent,
) : ViewModel() {

    private val navigation by navigationHandle<AgentEditDestination>()

    val state: ViewModelState<AgentEditState> = viewModelState(AgentEditState())

    init {
        val agentId = navigation.key.agentId
        if (agentId != null) {
            viewModelScope.launch {
                val agents = flowOfAgents().first()
                val agent = agents.firstOrNull { it.id.value == agentId }
                if (agent != null) {
                    state.update {
                        copy(
                            isNew = false,
                            name = agent.name,
                            workingDirectory = agent.workingDirectory,
                        )
                    }
                }
            }
        }
    }

    fun onNameChanged(name: String) {
        state.update { copy(name = name) }
    }

    fun onWorkingDirectoryChanged(dir: String) {
        state.update { copy(workingDirectory = dir) }
    }

    @OptIn(ExperimentalUuidApi::class)
    fun onSave() {
        val currentState = state.value
        if (currentState.name.isBlank() || currentState.workingDirectory.isBlank()) return

        val agentId = navigation.key.agentId ?: Uuid.random().toString()
        viewModelScope.launch {
            saveAgent(
                Agent(
                    id = Agent.Id(agentId),
                    name = currentState.name,
                    workingDirectory = currentState.workingDirectory,
                )
            )
            navigation.close()
        }
    }

    fun onCancel() {
        navigation.close()
    }
}
