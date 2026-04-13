package feature.agent.ui.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.enro.close
import dev.enro.navigationHandle
import dev.enro.open
import dev.isaacudy.udytils.state.ViewModelState
import dev.isaacudy.udytils.state.viewModelState
import feature.agent.domain.Agent
import feature.agent.domain.AgentOutput
import feature.agent.domain.AgentTask
import feature.agent.domain.FlowOfAgentState
import feature.agent.domain.FlowOfAgentTasks
import feature.agent.domain.FlowOfAgents
import feature.agent.domain.FlowOfCurrentTask
import feature.agent.domain.SendAgentTask
import feature.agent.domain.StopAgentTask
import feature.agent.ui.edit.AgentEditDestination
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant

class AgentDetailViewModel(
    private val flowOfAgents: FlowOfAgents,
    private val flowOfAgentState: FlowOfAgentState,
    private val flowOfCurrentTask: FlowOfCurrentTask,
    private val flowOfAgentTasks: FlowOfAgentTasks,
    private val sendAgentTask: SendAgentTask,
    private val stopAgentTask: StopAgentTask,
) : ViewModel() {

    private val navigation by navigationHandle<AgentDetailDestination>()

    val state: ViewModelState<AgentDetailState> = viewModelState(AgentDetailState())

    private val agentId get() = Agent.Id(navigation.key.agentId)

    init {
        viewModelScope.launch {
            flowOfAgents().collect { agents ->
                val agent = agents.firstOrNull { it.id == agentId }
                state.update { copy(agent = agent) }
            }
        }
        viewModelScope.launch {
            flowOfAgentState(agentId).collect { status ->
                state.update { copy(status = status) }
            }
        }
        viewModelScope.launch {
            flowOfCurrentTask(agentId).collect { task ->
                state.update {
                    copy(currentTask = task?.let {
                        it.copy(output = deduplicateOutput(it.output))
                    })
                }
            }
        }
        viewModelScope.launch {
            flowOfAgentTasks(agentId).collect { tasks ->
                state.update { copy(taskHistory = tasks) }
            }
        }
        viewModelScope.launch {
            while (true) {
                delay(1000)
                val task = state.value.currentTask ?: continue
                val startedAt = try {
                    Instant.parse(task.startedAt)
                } catch (_: Throwable) {
                    continue
                }
                val elapsed = Clock.System.now() - startedAt
                state.update { copy(elapsedSeconds = elapsed.inWholeSeconds) }
            }
        }
    }

    fun onPromptChanged(text: String) {
        state.update { copy(currentPrompt = text) }
    }

    fun onSendTask() {
        val prompt = state.value.currentPrompt.trim()
        if (prompt.isEmpty()) return
        state.update { copy(currentPrompt = "", elapsedSeconds = 0) }
        viewModelScope.launch {
            sendAgentTask(agentId, prompt, null)
        }
    }

    fun onStopTask() {
        viewModelScope.launch {
            stopAgentTask(agentId)
        }
    }

    fun onBack() {
        navigation.close()
    }

    fun onEditAgent() {
        navigation.open(AgentEditDestination(agentId = agentId.value))
    }

    fun onToggleOutputDetail() {
        state.update { copy(showFullOutput = !showFullOutput) }
    }
}

private fun deduplicateOutput(output: List<AgentOutput>): List<AgentOutput> {
    return output.filterIndexed { index, item ->
        if (item is AgentOutput.Result && index > 0) {
            val previous = output[index - 1]
            !(previous is AgentOutput.Text && previous.content == item.content)
        } else {
            true
        }
    }
}
