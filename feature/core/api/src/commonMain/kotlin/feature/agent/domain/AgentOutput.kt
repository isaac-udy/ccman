package feature.agent.domain

import kotlinx.serialization.Serializable

@Serializable
sealed interface AgentOutput {
    val rawJson: String

    @Serializable
    data class Text(val content: String, override val rawJson: String) : AgentOutput

    @Serializable
    data class Thinking(val content: String, override val rawJson: String) : AgentOutput

    @Serializable
    data class ToolUse(
        val name: String,
        val command: String,
        val description: String?,
        override val rawJson: String,
    ) : AgentOutput

    @Serializable
    data class ToolResult(val name: String, val output: String, override val rawJson: String) : AgentOutput

    @Serializable
    data class Unknown(val type: String, override val rawJson: String) : AgentOutput
}
