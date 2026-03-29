package feature.agent.data

import feature.agent.data.storage.AgentConfigEntity
import feature.agent.data.storage.AgentConfigStorage
import feature.agent.data.storage.ClaudeProcessStorage
import feature.agent.domain.Agent
import feature.agent.domain.AgentOutput
import feature.agent.domain.AgentStatus
import feature.agent.domain.DeleteAgent
import feature.agent.domain.FlowOfAgentOutput
import feature.agent.domain.FlowOfAgentState
import feature.agent.domain.FlowOfAgents
import feature.agent.domain.SaveAgent
import feature.agent.domain.SendAgentTask
import feature.agent.domain.StopAgentTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

internal class AgentRepository(
    private val agentConfigStorage: AgentConfigStorage,
    private val claudeProcessStorage: ClaudeProcessStorage,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val agentStatuses = MutableStateFlow<Map<Agent.Id, AgentStatus>>(emptyMap())
    private val agentOutputs = MutableStateFlow<Map<Agent.Id, List<AgentOutput>>>(emptyMap())

    val flowOfAgents = FlowOfAgents {
        agentConfigStorage.agents().map { entities ->
            entities.map { it.toAgent() }
        }
    }

    val saveAgent = SaveAgent { agent ->
        agentConfigStorage.save(agent.toEntity())
    }

    val deleteAgent = DeleteAgent { id ->
        agentConfigStorage.delete(id.value)
    }

    val flowOfAgentState = FlowOfAgentState { agentId ->
        agentStatuses.map { it[agentId] ?: AgentStatus.Idle }
    }

    val flowOfAgentOutput = FlowOfAgentOutput { agentId ->
        agentOutputs.map { it[agentId] ?: emptyList() }
    }

    val sendAgentTask = SendAgentTask { agentId, prompt ->
        val agents = agentConfigStorage.agents().first()
        val agent = agents.firstOrNull { it.id == agentId.value } ?: return@SendAgentTask

        agentStatuses.update { it + (agentId to AgentStatus.Running) }
        agentOutputs.update { it + (agentId to emptyList()) }

        scope.launch {
            try {
                claudeProcessStorage.startProcess(
                    agentId = agentId.value,
                    workingDirectory = agent.workingDirectory,
                    prompt = prompt,
                ).collect { line ->
                    val event = parseClaudeEvent(line) ?: return@collect
                    agentOutputs.update { current ->
                        val existing = current[agentId] ?: emptyList()
                        current + (agentId to (existing + event))
                    }
                }
                agentStatuses.update { it + (agentId to AgentStatus.Idle) }
            } catch (e: Throwable) {
                agentStatuses.update { it + (agentId to AgentStatus.Error(e.message ?: "Unknown error")) }
            }
        }
    }

    val stopAgentTask = StopAgentTask { agentId ->
        claudeProcessStorage.stopProcess(agentId.value)
        agentStatuses.update { it + (agentId to AgentStatus.Idle) }
    }
}

private fun AgentConfigEntity.toAgent(): Agent = Agent(
    id = Agent.Id(id),
    name = name,
    workingDirectory = workingDirectory,
)

private fun Agent.toEntity(): AgentConfigEntity = AgentConfigEntity(
    id = id.value,
    name = name,
    workingDirectory = workingDirectory,
)

private val lenientJson = Json { ignoreUnknownKeys = true }

private fun parseClaudeEvent(jsonLine: String): AgentOutput? {
    val line = jsonLine.trim()
    if (line.isEmpty()) return null

    val json = try {
        lenientJson.parseToJsonElement(line).jsonObject
    } catch (_: Throwable) {
        return null
    }

    val type = json["type"]?.jsonPrimitive?.contentOrNull ?: return null

    return when (type) {
        "assistant" -> parseAssistantMessage(json)
        "result" -> parseResultMessage(json)
        else -> null
    }
}

private fun parseAssistantMessage(json: JsonObject): AgentOutput? {
    val message = json["message"]?.jsonObject ?: return null
    val role = message["role"]?.jsonPrimitive?.contentOrNull
    if (role != "assistant") return null

    val content = message["content"]?.toString() ?: return null
    return AgentOutput.Text(content)
}

private fun parseResultMessage(json: JsonObject): AgentOutput? {
    val result = json["result"]?.jsonPrimitive?.contentOrNull ?: return null
    if (result.isBlank()) return null
    return AgentOutput.Text(result)
}
