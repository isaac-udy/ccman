package feature.agent.domain

import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline

@Serializable
data class Agent(
    val id: Id,
    val name: String,
    val workingDirectory: String,
    val instructions: String = "",
) {
    @Serializable
    @JvmInline
    value class Id(val value: String)
}
