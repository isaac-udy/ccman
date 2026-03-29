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
import kotlinx.serialization.json.jsonArray
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
                    val events = parseClaudeEvents(line)
                    if (events.isEmpty()) return@collect
                    agentOutputs.update { current ->
                        val existing = current[agentId] ?: emptyList()
                        current + (agentId to (existing + events))
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

private fun parseClaudeEvents(jsonLine: String): List<AgentOutput> {
    val line = jsonLine.trim()
    if (line.isEmpty()) return emptyList()

    val json = try {
        lenientJson.parseToJsonElement(line).jsonObject
    } catch (_: Throwable) {
        return emptyList()
    }

    val type = json["type"]?.jsonPrimitive?.contentOrNull ?: return emptyList()

    return when (type) {
        "assistant" -> parseAssistantMessage(json)
        "result" -> parseResultMessage(json)
        else -> emptyList()
    }
}

private fun parseAssistantMessage(json: JsonObject): List<AgentOutput> {
    val message = json["message"]?.jsonObject ?: return emptyList()
    val role = message["role"]?.jsonPrimitive?.contentOrNull
    if (role != "assistant") return emptyList()

    val contentArray = try {
        message["content"]?.jsonArray ?: return emptyList()
    } catch (_: Throwable) {
        return emptyList()
    }

    return contentArray.mapNotNull { element ->
        val block = try {
            element.jsonObject
        } catch (_: Throwable) {
            return@mapNotNull null
        }
        val blockType = block["type"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
        val raw = block.toString()
        when (blockType) {
            "text" -> {
                val text = block["text"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                if (text.isNotBlank()) AgentOutput.Text(text, rawJson = raw) else null
            }
            "thinking" -> {
                val thinking = block["thinking"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
                if (thinking.isNotBlank()) AgentOutput.Thinking(thinking, rawJson = raw) else null
            }
            "tool_use" -> parseToolUseBlock(block)
            else -> AgentOutput.Unknown(type = blockType, rawJson = raw)
        }
    }
}

private fun parseToolUseBlock(block: JsonObject): AgentOutput.ToolUse? {
    val name = block["name"]?.jsonPrimitive?.contentOrNull ?: return null
    val input = try {
        block["input"]?.jsonObject
    } catch (_: Throwable) {
        null
    }

    val description = input?.get("description")?.jsonPrimitive?.contentOrNull

    val command = if (input != null) {
        val commandValue = input["command"]?.jsonPrimitive?.contentOrNull
        if (commandValue != null && input.keys.all { it == "command" || it == "description" }) {
            commandValue
        } else {
            input.toString()
        }
    } else {
        block["input"]?.toString() ?: ""
    }

    return AgentOutput.ToolUse(
        name = name,
        command = command,
        description = description,
        rawJson = block.toString(),
    )
}

private fun parseResultMessage(json: JsonObject): List<AgentOutput> {
    val result = json["result"]?.jsonPrimitive?.contentOrNull ?: return emptyList()
    if (result.isBlank()) return emptyList()
    return listOf(AgentOutput.Result(result, rawJson = json.toString()))
}
