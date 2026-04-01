package feature.agent.domain

fun interface DisconnectSlack {
    suspend operator fun invoke()
}
