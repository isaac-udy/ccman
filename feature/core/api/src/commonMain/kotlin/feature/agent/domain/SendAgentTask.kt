package feature.agent.domain

fun interface SendAgentTask {
    suspend operator fun invoke(agentId: Agent.Id, prompt: String, sessionId: String?)
}
