package feature.agent.domain

fun interface DeleteAgent {
    suspend operator fun invoke(id: Agent.Id)
}
