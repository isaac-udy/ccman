package feature.agent.domain

fun interface ConnectSlack {
    suspend operator fun invoke()
}
