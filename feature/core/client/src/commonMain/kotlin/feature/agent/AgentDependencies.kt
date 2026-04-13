package feature.agent

import feature.agent.data.AgentRepository
import feature.agent.data.SlackRepository
import feature.agent.data.storage.AgentConfigStorage
import feature.agent.data.storage.AgentTaskStorage
import feature.agent.data.storage.ClaudeProcessStorage
import feature.agent.data.storage.SlackConfigStorage
import feature.agent.data.storage.SlackServiceStorage
import feature.agent.data.storage.SlackThreadSessionStorage
import feature.agent.domain.ConnectSlack
import feature.agent.domain.DeleteAgent
import feature.agent.domain.DisconnectSlack
import feature.agent.domain.FlowOfAgentState
import feature.agent.domain.FlowOfAgentTasks
import feature.agent.domain.FlowOfAgents
import feature.agent.domain.FlowOfCurrentTask
import feature.agent.domain.FlowOfSlackConfig
import feature.agent.domain.FlowOfSlackConnectionStatus
import feature.agent.domain.FlowOfSlackMessages
import feature.agent.domain.FlowOfSlackQueue
import feature.agent.domain.SendSlackTestMessage
import feature.agent.domain.SaveAgent
import feature.agent.domain.SaveSlackConfig
import feature.agent.domain.SendAgentTask
import feature.agent.domain.StopAgentTask
import feature.agent.ui.detail.AgentDetailViewModel
import feature.agent.ui.edit.AgentEditViewModel
import feature.agent.ui.list.AgentListViewModel
import feature.agent.ui.settings.SettingsViewModel
import feature.agent.ui.slack.SlackDetailViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val agentDependencies = module {
    singleOf(::AgentConfigStorage)
    singleOf(::AgentTaskStorage)
    singleOf(::ClaudeProcessStorage)
    singleOf(::AgentRepository)

    single<FlowOfAgents> { get<AgentRepository>().flowOfAgents }
    single<SaveAgent> { get<AgentRepository>().saveAgent }
    single<DeleteAgent> { get<AgentRepository>().deleteAgent }
    single<FlowOfAgentState> { get<AgentRepository>().flowOfAgentState }
    single<FlowOfCurrentTask> { get<AgentRepository>().flowOfCurrentTask }
    single<FlowOfAgentTasks> { get<AgentRepository>().flowOfAgentTasks }
    single<SendAgentTask> { get<AgentRepository>().sendAgentTask }
    single<StopAgentTask> { get<AgentRepository>().stopAgentTask }

    singleOf(::SlackConfigStorage)
    singleOf(::SlackServiceStorage)
    singleOf(::SlackThreadSessionStorage)
    singleOf(::SlackRepository)

    single<FlowOfSlackConfig> { get<SlackRepository>().flowOfSlackConfig }
    single<SaveSlackConfig> { get<SlackRepository>().saveSlackConfig }
    single<FlowOfSlackConnectionStatus> { get<SlackRepository>().flowOfSlackConnectionStatus }
    single<FlowOfSlackQueue> { get<SlackRepository>().flowOfSlackQueue }
    single<FlowOfSlackMessages> { get<SlackRepository>().flowOfSlackMessages }
    single<SendSlackTestMessage> { get<SlackRepository>().sendSlackTestMessage }
    single<ConnectSlack> { get<SlackRepository>().connectSlack }
    single<DisconnectSlack> { get<SlackRepository>().disconnectSlack }

    viewModelOf(::AgentListViewModel)
    viewModelOf(::AgentDetailViewModel)
    viewModelOf(::AgentEditViewModel)
    viewModelOf(::SettingsViewModel)
    viewModelOf(::SlackDetailViewModel)
}
