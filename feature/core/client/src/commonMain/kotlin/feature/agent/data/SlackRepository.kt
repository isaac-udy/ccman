package feature.agent.data

import feature.agent.data.storage.ChannelBindingEntity
import feature.agent.data.storage.SlackConfigEntity
import feature.agent.data.storage.SlackConfigStorage
import feature.agent.data.storage.SlackServiceStorage
import feature.agent.domain.Agent
import feature.agent.domain.AgentStatus
import feature.agent.domain.ConnectSlack
import feature.agent.domain.DisconnectSlack
import feature.agent.domain.FlowOfAgentState
import feature.agent.domain.FlowOfAgents
import feature.agent.domain.FlowOfCurrentTask
import feature.agent.domain.FlowOfSlackConfig
import feature.agent.domain.FlowOfSlackConnectionStatus
import feature.agent.domain.FlowOfSlackMessages
import feature.agent.domain.FlowOfSlackQueue
import feature.agent.domain.SaveSlackConfig
import feature.agent.domain.SendAgentTask
import feature.agent.domain.SendSlackTestMessage
import feature.agent.domain.SlackMessageLog
import feature.agent.domain.SlackConfig
import feature.agent.domain.SlackConnectionStatus
import feature.agent.domain.SlackQueueEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

@OptIn(ExperimentalUuidApi::class, ExperimentalCoroutinesApi::class)
internal class SlackRepository(
    private val slackConfigStorage: SlackConfigStorage,
    private val slackServiceStorage: SlackServiceStorage,
    private val flowOfAgents: FlowOfAgents,
    private val flowOfAgentState: FlowOfAgentState,
    private val sendAgentTask: SendAgentTask,
    private val flowOfCurrentTask: FlowOfCurrentTask,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val connectionStatus = MutableStateFlow<SlackConnectionStatus>(SlackConnectionStatus.Disconnected)
    private val queue = MutableStateFlow<List<SlackQueueEntry>>(emptyList())
    private val channelIdToName = MutableStateFlow<Map<String, String>>(emptyMap()) // channelId -> channelName
    private val channelNameToGroup = MutableStateFlow<Map<String, String>>(emptyMap()) // channelName -> agentGroup
    private val messageLog = MutableStateFlow<List<SlackMessageLog>>(emptyList())

    val flowOfSlackConfig = FlowOfSlackConfig {
        slackConfigStorage.config().map { entity ->
            entity?.let {
                SlackConfig(
                    botToken = it.botToken,
                    appToken = it.appToken,
                    enabled = it.enabled,
                    channelBindings = it.channelBindings.map { b ->
                        SlackConfig.ChannelBinding(b.channelName, b.group)
                    },
                )
            }
        }
    }

    val saveSlackConfig = SaveSlackConfig { config ->
        slackConfigStorage.save(
            SlackConfigEntity(
                botToken = config.botToken,
                appToken = config.appToken,
                enabled = config.enabled,
                channelBindings = config.channelBindings.map { b ->
                    ChannelBindingEntity(b.channelName, b.group)
                },
            )
        )
    }

    val flowOfSlackConnectionStatus = FlowOfSlackConnectionStatus { connectionStatus }

    val flowOfSlackQueue = FlowOfSlackQueue { queue }

    val connectSlack = ConnectSlack {
        val config = slackConfigStorage.config().first() ?: error("Slack not configured")
        doConnect(config.botToken, config.appToken)
    }

    val flowOfSlackMessages = FlowOfSlackMessages { messageLog }

    val sendSlackTestMessage = SendSlackTestMessage {
        val channels = try {
            slackServiceStorage.listChannels()
        } catch (_: Throwable) {
            emptyList()
        }
        val testChannel = channels.firstOrNull { it.name == "ccman-test" }
        if (testChannel != null) {
            slackServiceStorage.postMessage(
                channelId = testChannel.id,
                text = "Hello from CCMan! This is a test message.",
                threadTs = null,
            )
        } else {
            val created = slackServiceStorage.createChannel("ccman-test")
            slackServiceStorage.postMessage(
                channelId = created.id,
                text = "Hello from CCMan! This is a test message.",
                threadTs = null,
            )
        }
    }

    val disconnectSlack = DisconnectSlack {
        slackServiceStorage.disconnect()
        connectionStatus.value = SlackConnectionStatus.Disconnected
    }

    init {
        scope.launch {
            val config = flowOfSlackConfig().first()
            if (config != null && config.botToken.isNotBlank() && config.appToken.isNotBlank() && config.enabled) {
                try {
                    connectSlack()
                } catch (_: Throwable) {
                }
            }
        }
    }

    private fun doConnect(botToken: String, appToken: String) {
        connectionStatus.value = SlackConnectionStatus.Connecting

        scope.launch {
            try {
                startIdleAgentMonitor()

                // connect() returns a cold flow; collecting it starts the socket mode connection.
                // The adapter has a short delay after starting to let the connection establish.
                slackServiceStorage.connect(botToken, appToken).collect { message ->
                    if (!channelIdToName.value.containsKey(message.channelId)) {
                        resolveChannelMappings()
                    }
                    if (connectionStatus.value !is SlackConnectionStatus.Connected) {
                        connectionStatus.value = SlackConnectionStatus.Connected
                    }
                    val channelName = channelIdToName.value[message.channelId]
                        ?: message.channelId

                    messageLog.update { log ->
                        log + SlackMessageLog(
                            channelId = message.channelId,
                            channelName = channelName,
                            userId = message.userId,
                            text = message.text,
                            timestamp = Clock.System.now().toString(),
                        )
                    }
                    onSlackMessage(message.channelId, message.threadTs, message.messageTs, message.text)
                }

                connectionStatus.value = SlackConnectionStatus.Disconnected
            } catch (e: Throwable) {
                e.printStackTrace()
                connectionStatus.value = SlackConnectionStatus.Error(e.message ?: "Unknown error")
                delay(5000)
                doConnect(botToken, appToken)
            }
        }

        // Set connected after a brief delay to allow the socket to establish
        scope.launch {
            delay(3000)
            if (connectionStatus.value is SlackConnectionStatus.Connecting) {
                connectionStatus.value = SlackConnectionStatus.Connected
            }
        }
    }

    private suspend fun resolveChannelMappings() {
        val config = slackConfigStorage.config().first()
        val bindings = config?.channelBindings ?: emptyList()
        channelNameToGroup.value = bindings.associate { it.channelName to it.group }

        val existingChannels = try {
            slackServiceStorage.listChannels()
        } catch (_: Throwable) {
            emptyList()
        }
        channelIdToName.value = existingChannels.associate { it.id to it.name }
    }

    private fun onSlackMessage(channelId: String, threadTs: String?, messageTs: String, text: String) {
        val channelName = channelIdToName.value[channelId] ?: channelId
        val group = channelNameToGroup.value[channelName] ?: return // no binding configured, ignore
        val entry = SlackQueueEntry(
            id = SlackQueueEntry.Id(Uuid.random().toString()),
            channelId = channelId,
            channelName = channelName,
            threadTs = threadTs ?: messageTs,
            messageTs = messageTs,
            prompt = text,
            status = SlackQueueEntry.Status.Queued,
            queuedAt = Clock.System.now().toString(),
        )

        queue.update { it + entry }
        scope.launch { tryDispatchNext(channelName) }
    }

    private suspend fun tryDispatchNext(channelName: String) {
        val pendingEntry = queue.value
            .firstOrNull { it.channelName == channelName && it.status == SlackQueueEntry.Status.Queued }
            ?: return

        val group = channelNameToGroup.value[channelName] ?: return
        val agents = flowOfAgents().first().filter { it.group == group }
        val idleAgent = agents.firstOrNull { agent ->
            flowOfAgentState(agent.id).first() is AgentStatus.Idle
        } ?: return

        val now = Clock.System.now()
        val replyTs = try {
            slackServiceStorage.postMessage(
                channelId = pendingEntry.channelId,
                text = "Processing started on ${idleAgent.name} at ${formatTime(now)}...",
                threadTs = pendingEntry.threadTs,
            )
        } catch (_: Throwable) {
            null
        }

        val updatedEntry = pendingEntry.copy(
            status = SlackQueueEntry.Status.Dispatched,
            agentId = idleAgent.id,
            replyTs = replyTs,
        )
        queue.update { entries ->
            entries.map { if (it.id == pendingEntry.id) updatedEntry else it }
        }

        scope.launch {
            try {
                sendAgentTask(idleAgent.id, updatedEntry.prompt)
                onTaskCompleted(updatedEntry, idleAgent)
            } catch (e: Throwable) {
                onTaskFailed(updatedEntry, idleAgent, e.message ?: "Unknown error")
            }
        }
    }

    private suspend fun onTaskCompleted(entry: SlackQueueEntry, agent: Agent) {
        val tasks = flowOfCurrentTask(agent.id).first()
        // Task is null because it completed and was cleared — get last completed task info
        // The result text comes from the task that just finished. Since sendAgentTask is a suspending
        // call that returns after the task completes, we can look at the persisted task history.
        // For now, we update the Slack message with a generic completion message.
        // The actual result can be retrieved by checking the last task's output.

        val completedAt = Clock.System.now()
        val startedAt = try {
            Instant.parse(entry.queuedAt)
        } catch (_: Throwable) {
            completedAt
        }
        val durationSeconds = (completedAt - startedAt).inWholeSeconds
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        val durationText = if (mins > 0) "$mins mins $secs seconds" else "$secs seconds"

        queue.update { entries ->
            entries.map {
                if (it.id == entry.id) it.copy(status = SlackQueueEntry.Status.Completed) else it
            }
        }

        val completedReplyTs = entry.replyTs
        if (completedReplyTs != null) {
            try {
                slackServiceStorage.updateMessage(
                    channelId = entry.channelId,
                    ts = completedReplyTs,
                    text = "Completed by ${agent.name} in $durationText",
                )
            } catch (_: Throwable) {
            }
        }

        tryDispatchNext(entry.channelName)
    }

    private suspend fun onTaskFailed(entry: SlackQueueEntry, agent: Agent, error: String) {
        val completedAt = Clock.System.now()
        val startedAt = try {
            Instant.parse(entry.queuedAt)
        } catch (_: Throwable) {
            completedAt
        }
        val durationSeconds = (completedAt - startedAt).inWholeSeconds
        val mins = durationSeconds / 60
        val secs = durationSeconds % 60
        val durationText = if (mins > 0) "$mins mins $secs seconds" else "$secs seconds"

        queue.update { entries ->
            entries.map {
                if (it.id == entry.id) it.copy(status = SlackQueueEntry.Status.Error) else it
            }
        }

        val errorReplyTs = entry.replyTs
        if (errorReplyTs != null) {
            try {
                slackServiceStorage.updateMessage(
                    channelId = entry.channelId,
                    ts = errorReplyTs,
                    text = "Error on ${agent.name} after $durationText: $error",
                )
            } catch (_: Throwable) {
            }
        }

        tryDispatchNext(entry.channelName)
    }

    private fun startIdleAgentMonitor() {
        scope.launch {
            flowOfAgents().flatMapLatest { agents ->
                if (agents.isEmpty()) return@flatMapLatest flowOf(emptyList<Pair<Agent, AgentStatus>>())
                combine(
                    agents.map { agent ->
                        flowOfAgentState(agent.id).map { status -> agent to status }
                    }
                ) { it.toList() }
            }.collect { agentStates ->
                for ((agent, status) in agentStates) {
                    if (status is AgentStatus.Idle && agent.group.isNotBlank()) {
                        // Find channel names bound to this agent's group
                        val boundChannelNames = channelNameToGroup.value
                            .filter { (_, group) -> group == agent.group }
                            .keys
                        for (channelName in boundChannelNames) {
                            val hasQueued = queue.value.any {
                                it.channelName == channelName && it.status == SlackQueueEntry.Status.Queued
                            }
                            if (hasQueued) {
                                tryDispatchNext(channelName)
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatTime(instant: Instant): String {
    val str = instant.toString()
    val time = str.substringAfter("T").substringBefore(".").take(5)
    return time
}
