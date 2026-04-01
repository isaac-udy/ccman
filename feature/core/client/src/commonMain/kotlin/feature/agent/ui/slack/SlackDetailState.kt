package feature.agent.ui.slack

import feature.agent.domain.SlackConnectionStatus
import feature.agent.domain.SlackMessageLog

data class SlackDetailState(
    val connectionStatus: SlackConnectionStatus = SlackConnectionStatus.Disconnected,
    val messages: List<SlackMessageLog> = emptyList(),
)
