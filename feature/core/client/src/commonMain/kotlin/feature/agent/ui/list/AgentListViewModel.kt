package feature.agent.ui.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.enro.navigationHandle
import dev.enro.open
import dev.isaacudy.udytils.state.ViewModelState
import dev.isaacudy.udytils.state.viewModelState
import feature.agent.domain.Agent
import feature.agent.domain.FlowOfAgentState
import feature.agent.domain.FlowOfAgents
import feature.agent.domain.FlowOfSlackConfig
import feature.agent.domain.FlowOfSlackConnectionStatus
import feature.agent.domain.SlackConnectionStatus
import feature.agent.ui.detail.AgentDetailDestination
import feature.agent.ui.edit.AgentEditDestination
import feature.agent.ui.settings.SettingsDestination
import feature.agent.ui.slack.SlackDetailDestination
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class AgentListViewModel(
    private val flowOfAgents: FlowOfAgents,
    private val flowOfAgentState: FlowOfAgentState,
    private val flowOfSlackConfig: FlowOfSlackConfig,
    private val flowOfSlackConnectionStatus: FlowOfSlackConnectionStatus,
) : ViewModel() {

    private val navigation by navigationHandle<AgentListDestination>()

    val state: ViewModelState<AgentListState> = viewModelState(AgentListState())

    init {
        viewModelScope.launch {
            flowOfAgents().flatMapLatest { agents ->
                if (agents.isEmpty()) {
                    flowOf(emptyList())
                } else {
                    combine(
                        agents.map { agent ->
                            flowOfAgentState(agent.id).map { status ->
                                AgentListState.AgentListEntry(agent, status)
                            }
                        }
                    ) { entries -> entries.toList() }
                }
            }.collect { entries ->
                val grouped = entries
                    .groupBy { it.agent.group.ifBlank { "Ungrouped" } }
                    .map { (groupName, groupEntries) ->
                        AgentListState.AgentGroup(name = groupName, agents = groupEntries)
                    }
                    .sortedBy { if (it.name == "Ungrouped") "\uFFFF" else it.name }
                state.update { copy(groups = grouped) }
            }
        }

        viewModelScope.launch {
            flowOfSlackConnectionStatus().combine(flowOfSlackConfig()) { status, config ->
                when {
                    config == null || config.botToken.isBlank() || config.appToken.isBlank() -> {
                        AgentListState.SlackStatus.NotConfigured
                    }
                    status is SlackConnectionStatus.Connected -> {
                        AgentListState.SlackStatus.Connected
                    }
                    status is SlackConnectionStatus.Connecting -> {
                        AgentListState.SlackStatus.Connecting
                    }
                    status is SlackConnectionStatus.Error -> {
                        AgentListState.SlackStatus.Error(status.message)
                    }
                    else -> {
                        AgentListState.SlackStatus.Disconnected
                    }
                }
            }.collect { slackStatus ->
                state.update { copy(slackStatus = slackStatus) }
            }
        }
    }

    fun onAgentSelected(agentId: Agent.Id) {
        navigation.open(AgentDetailDestination(agentId = agentId.value))
    }

    fun onCreateAgent() {
        navigation.open(AgentEditDestination(agentId = null))
    }

    fun onOpenSettings() {
        navigation.open(SettingsDestination)
    }

    fun onOpenSlackDetails() {
        navigation.open(SlackDetailDestination)
    }
}
