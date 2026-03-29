package feature.agent.domain

fun interface SaveAgent {
    suspend operator fun invoke(agent: Agent)
}
