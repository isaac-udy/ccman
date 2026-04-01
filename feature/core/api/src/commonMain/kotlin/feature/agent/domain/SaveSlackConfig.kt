package feature.agent.domain

fun interface SaveSlackConfig {
    suspend operator fun invoke(config: SlackConfig)
}
