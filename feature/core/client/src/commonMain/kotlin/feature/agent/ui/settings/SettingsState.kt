package feature.agent.ui.settings

import feature.agent.domain.SlackConnectionStatus

data class SettingsState(
    val slackBotToken: String = "",
    val slackAppToken: String = "",
    val slackEnabled: Boolean = false,
    val slackConnectionStatus: SlackConnectionStatus = SlackConnectionStatus.Disconnected,
    val slackConnecting: Boolean = false,
)
