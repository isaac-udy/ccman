package feature.agent.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.enro.close
import dev.enro.navigationHandle
import dev.isaacudy.udytils.state.ViewModelState
import dev.isaacudy.udytils.state.viewModelState
import feature.agent.domain.ConnectSlack
import feature.agent.domain.DisconnectSlack
import feature.agent.domain.FlowOfAgents
import feature.agent.domain.FlowOfSlackConfig
import feature.agent.domain.FlowOfSlackConnectionStatus
import feature.agent.domain.FlowOfSlackMessages
import feature.agent.domain.SaveSlackConfig
import feature.agent.domain.SendSlackTestMessage
import feature.agent.domain.SlackConfig
import feature.agent.domain.SlackConnectionStatus
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val flowOfSlackConfig: FlowOfSlackConfig,
    private val saveSlackConfig: SaveSlackConfig,
    private val flowOfSlackConnectionStatus: FlowOfSlackConnectionStatus,
    private val flowOfSlackMessages: FlowOfSlackMessages,
    private val flowOfAgents: FlowOfAgents,
    private val sendSlackTestMessage: SendSlackTestMessage,
    private val connectSlack: ConnectSlack,
    private val disconnectSlack: DisconnectSlack,
) : ViewModel() {

    private val navigation by navigationHandle<SettingsDestination>()

    val state: ViewModelState<SettingsState> = viewModelState(SettingsState())

    init {
        viewModelScope.launch {
            flowOfSlackConfig().collect { config ->
                if (config != null) {
                    state.update {
                        copy(
                            slackBotToken = config.botToken,
                            slackAppToken = config.appToken,
                            slackEnabled = config.enabled,
                            channelBindings = config.channelBindings.map {
                                SettingsState.ChannelBindingEntry(it.channelName, it.group)
                            },
                        )
                    }
                }
            }
        }
        viewModelScope.launch {
            flowOfSlackConnectionStatus().collect { status ->
                state.update {
                    copy(
                        slackConnectionStatus = status,
                        slackConnecting = status is SlackConnectionStatus.Connecting,
                    )
                }
            }
        }
        viewModelScope.launch {
            flowOfSlackMessages().collect { messages ->
                state.update { copy(slackMessages = messages) }
            }
        }
        viewModelScope.launch {
            flowOfAgents().collect { agents ->
                val groups = agents.map { it.group }.filter { it.isNotBlank() }.distinct().sorted()
                state.update { copy(availableGroups = groups) }
            }
        }
    }

    fun onSlackBotTokenChanged(token: String) {
        state.update { copy(slackBotToken = token) }
    }

    fun onSlackAppTokenChanged(token: String) {
        state.update { copy(slackAppToken = token) }
    }

    fun onSlackEnabledChanged(enabled: Boolean) {
        state.update { copy(slackEnabled = enabled) }
    }

    fun onNewBindingChannelChanged(channel: String) {
        state.update { copy(newBindingChannel = channel) }
    }

    fun onNewBindingGroupChanged(group: String) {
        state.update { copy(newBindingGroup = group) }
    }

    fun onAddBinding() {
        val current = state.value
        if (current.newBindingChannel.isBlank() || current.newBindingGroup.isBlank()) return
        state.update {
            copy(
                channelBindings = channelBindings + SettingsState.ChannelBindingEntry(
                    channelName = current.newBindingChannel.trim(),
                    group = current.newBindingGroup.trim(),
                ),
                newBindingChannel = "",
                newBindingGroup = "",
            )
        }
        saveCurrentConfig()
    }

    fun onRemoveBinding(index: Int) {
        state.update {
            copy(channelBindings = channelBindings.filterIndexed { i, _ -> i != index })
        }
        saveCurrentConfig()
    }

    fun onSaveSlackConfig() {
        saveCurrentConfig()
    }

    private fun saveCurrentConfig() {
        val currentState = state.value
        viewModelScope.launch {
            saveSlackConfig(
                SlackConfig(
                    botToken = currentState.slackBotToken,
                    appToken = currentState.slackAppToken,
                    enabled = currentState.slackEnabled,
                    channelBindings = currentState.channelBindings.map {
                        SlackConfig.ChannelBinding(it.channelName, it.group)
                    },
                )
            )
        }
    }

    fun onConnectSlack() {
        saveCurrentConfig()
        viewModelScope.launch {
            try {
                connectSlack()
            } catch (_: Throwable) {
            }
        }
    }

    fun onDisconnectSlack() {
        viewModelScope.launch {
            disconnectSlack()
        }
    }

    fun onSendTestMessage() {
        viewModelScope.launch {
            try {
                sendSlackTestMessage()
            } catch (_: Throwable) {
            }
        }
    }

    fun onBack() {
        navigation.close()
    }
}
