package feature.agent.ui.settings

import feature.agent.domain.SlackConnectionStatus
import feature.agent.domain.SlackMessageLog

data class SettingsState(
    val slackBotToken: String = "",
    val slackAppToken: String = "",
    val slackEnabled: Boolean = false,
    val slackConnectionStatus: SlackConnectionStatus = SlackConnectionStatus.Disconnected,
    val slackConnecting: Boolean = false,
    val slackMessages: List<SlackMessageLog> = emptyList(),
)
