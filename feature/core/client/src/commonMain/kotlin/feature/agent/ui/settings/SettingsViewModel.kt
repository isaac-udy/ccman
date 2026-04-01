package feature.agent.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.enro.close
import dev.enro.navigationHandle
import dev.isaacudy.udytils.state.ViewModelState
import dev.isaacudy.udytils.state.viewModelState
import feature.agent.domain.ConnectSlack
import feature.agent.domain.DisconnectSlack
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

    fun onSaveSlackConfig() {
        val currentState = state.value
        viewModelScope.launch {
            saveSlackConfig(
                SlackConfig(
                    botToken = currentState.slackBotToken,
                    appToken = currentState.slackAppToken,
                    enabled = currentState.slackEnabled,
                )
            )
        }
    }

    fun onConnectSlack() {
        onSaveSlackConfig()
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
