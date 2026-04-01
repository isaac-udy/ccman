package feature.agent.ui.slack

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.enro.close
import dev.enro.navigationHandle
import dev.isaacudy.udytils.state.ViewModelState
import dev.isaacudy.udytils.state.viewModelState
import feature.agent.domain.FlowOfSlackConnectionStatus
import feature.agent.domain.FlowOfSlackMessages
import kotlinx.coroutines.launch

class SlackDetailViewModel(
    private val flowOfSlackConnectionStatus: FlowOfSlackConnectionStatus,
    private val flowOfSlackMessages: FlowOfSlackMessages,
) : ViewModel() {

    private val navigation by navigationHandle<SlackDetailDestination>()

    val state: ViewModelState<SlackDetailState> = viewModelState(SlackDetailState())

    init {
        viewModelScope.launch {
            flowOfSlackConnectionStatus().collect { status ->
                state.update { copy(connectionStatus = status) }
            }
        }
        viewModelScope.launch {
            flowOfSlackMessages().collect { messages ->
                state.update { copy(messages = messages) }
            }
        }
    }

    fun onBack() {
        navigation.close()
    }
}
