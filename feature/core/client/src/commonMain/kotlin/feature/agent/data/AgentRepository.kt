package feature.agent.data

import feature.agent.data.storage.AgentConfigEntity
import feature.agent.data.storage.AgentConfigStorage
import feature.agent.data.storage.AgentTaskEntity
import feature.agent.data.storage.AgentTaskStorage
import feature.agent.data.storage.ClaudeProcessStorage
import feature.agent.domain.Agent
import feature.agent.domain.AgentOutput
import feature.agent.domain.AgentStatus
import feature.agent.domain.AgentTask
import feature.agent.domain.DeleteAgent
import feature.agent.domain.FlowOfAgentOutput
import feature.agent.domain.FlowOfAgentState
import feature.agent.domain.FlowOfAgentTasks
import feature.agent.domain.FlowOfAgents
import feature.agent.domain.SaveAgent
import feature.agent.domain.SendAgentTask
import feature.agent.domain.StopAgentTask
import kotlin.time.Clock
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
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

internal class AgentRepository(
    private val agentConfigStorage: AgentConfigStorage,
    private val agentTaskStorage: AgentTaskStorage,
    private val claudeProcessStorage: ClaudeProcessStorage,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val agentStatuses = MutableStateFlow<Map<Agent.Id, AgentStatus>>(emptyMap())
    private val currentTasks = MutableStateFlow<Map<Agent.Id, AgentTask>>(emptyMap())

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
        currentTasks.map { it[agentId]?.output ?: emptyList() }
    }

    val flowOfAgentTasks = FlowOfAgentTasks { agentId ->
        agentTaskStorage.tasksForAgent(agentId.value).map { entities ->
            entities.map { it.toAgentTask() }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    val sendAgentTask = SendAgentTask { agentId, prompt ->
        val agents = agentConfigStorage.agents().first()
        val agent = agents.firstOrNull { it.id == agentId.value } ?: return@SendAgentTask

        val taskId = AgentTask.Id(Uuid.random().toString())
        val now = Clock.System.now().toString()

        val fullPrompt = buildString {
            if (agent.instructions.isNotBlank()) {
                append(agent.instructions)
                append("\n\n")
            }
            append(prompt)
        }

        val task = AgentTask(
            id = taskId,
            agentId = agentId,
            prompt = prompt,
            output = emptyList(),
            status = AgentTask.Status.Running,
            startedAt = now,
            completedAt = null,
        )

        currentTasks.update { it + (agentId to task) }
        agentStatuses.update { it + (agentId to AgentStatus.Running) }

        scope.launch {
            try {
                claudeProcessStorage.startProcess(
                    agentId = agentId.value,
                    workingDirectory = agent.workingDirectory,
                    prompt = fullPrompt,
                ).collect { line ->
                    val events = parseClaudeEvents(line)
                    if (events.isEmpty()) return@collect
                    currentTasks.update { current ->
                        val existing = current[agentId] ?: return@update current
                        current + (agentId to existing.copy(output = existing.output + events))
                    }
                }
                val completedTask = currentTasks.value[agentId]?.copy(
                    status = AgentTask.Status.Completed,
                    completedAt = Clock.System.now().toString(),
                )
                if (completedTask != null) {
                    agentTaskStorage.save(completedTask.toEntity())
                }
                currentTasks.update { it - agentId }
                agentStatuses.update { it + (agentId to AgentStatus.Idle) }
            } catch (e: Throwable) {
                val errorTask = currentTasks.value[agentId]?.copy(
                    status = AgentTask.Status.Error,
                    completedAt = Clock.System.now().toString(),
                )
                if (errorTask != null) {
                    agentTaskStorage.save(errorTask.toEntity())
                }
                currentTasks.update { it - agentId }
                agentStatuses.update { it + (agentId to AgentStatus.Error(e.message ?: "Unknown error")) }
            }
        }
    }

    val stopAgentTask = StopAgentTask { agentId ->
        claudeProcessStorage.stopProcess(agentId.value)
        val stoppedTask = currentTasks.value[agentId]?.copy(
            status = AgentTask.Status.Error,
            completedAt = Clock.System.now().toString(),
        )
        if (stoppedTask != null) {
            scope.launch { agentTaskStorage.save(stoppedTask.toEntity()) }
        }
        currentTasks.update { it - agentId }
        agentStatuses.update { it + (agentId to AgentStatus.Idle) }
    }
}

private fun AgentConfigEntity.toAgent(): Agent = Agent(
    id = Agent.Id(id),
    name = name,
    workingDirectory = workingDirectory,
    instructions = instructions,
)

private fun Agent.toEntity(): AgentConfigEntity = AgentConfigEntity(
    id = id.value,
    name = name,
    workingDirectory = workingDirectory,
    instructions = instructions,
)

private fun AgentTaskEntity.toAgentTask(): AgentTask = AgentTask(
    id = AgentTask.Id(id),
    agentId = Agent.Id(agentId),
    prompt = prompt,
    output = output,
    status = when (status) {
        "Completed" -> AgentTask.Status.Completed
        "Error" -> AgentTask.Status.Error
        else -> AgentTask.Status.Running
    },
    startedAt = startedAt,
    completedAt = completedAt,
)

private fun AgentTask.toEntity(): AgentTaskEntity = AgentTaskEntity(
    id = id.value,
    agentId = agentId.value,
    prompt = prompt,
    output = output,
    status = status.name,
    startedAt = startedAt,
    completedAt = completedAt,
)

private val lenientJson = Json { ignoreUnknownKeys = true }

private fun parseClaudeEvents(jsonLine: String): List<AgentOutput> {
    val line = jsonLine.trim()
    if (line.isEmpty()) return emptyList()

    println("output: "+jsonLine)
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
