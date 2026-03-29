package feature.agent.domain

fun interface StopAgentTask {
    suspend operator fun invoke(agentId: Agent.Id)
}
