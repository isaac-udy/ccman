package feature.agent.domain

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class AgentTask(
    val id: Id,
    val agentId: Agent.Id,
    val prompt: String,
    val output: List<AgentOutput>,
    val status: Status,
    val startedAt: String,
    val completedAt: String?,
) {
    @Serializable
    @JvmInline
    value class Id(val value: String)

    @Serializable
    enum class Status { Running, Completed, Error }
}
