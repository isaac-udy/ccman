package feature.agent.domain

fun interface SendSlackTestMessage {
    suspend operator fun invoke()
}
