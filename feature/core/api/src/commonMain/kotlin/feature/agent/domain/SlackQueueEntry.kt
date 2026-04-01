package feature.agent.domain

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class SlackQueueEntry(
    val id: Id,
    val channelId: String,
    val channelName: String,
    val threadTs: String,
    val messageTs: String,
    val prompt: String,
    val status: Status,
    val agentId: Agent.Id? = null,
    val replyTs: String? = null,
    val queuedAt: String,
) {
    @Serializable
    @JvmInline
    value class Id(val value: String)

    @Serializable
    enum class Status { Queued, Dispatched, Completed, Error }
}
