package feature.agent.domain

import kotlinx.serialization.Serializable

@Serializable
sealed interface AgentOutput {
    @Serializable
    data class Text(val content: String) : AgentOutput

    @Serializable
    data class Thinking(val content: String) : AgentOutput

    @Serializable
    data class ToolUse(val name: String, val input: String) : AgentOutput

    @Serializable
    data class ToolResult(val name: String, val output: String) : AgentOutput

    @Serializable
    data class Unknown(val type: String, val rawJson: String) : AgentOutput
}
