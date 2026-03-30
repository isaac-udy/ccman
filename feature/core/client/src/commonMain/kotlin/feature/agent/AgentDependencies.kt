package feature.agent

import feature.agent.data.AgentRepository
import feature.agent.data.storage.AgentConfigStorage
import feature.agent.data.storage.AgentTaskStorage
import feature.agent.data.storage.ClaudeProcessStorage
import feature.agent.domain.DeleteAgent
import feature.agent.domain.FlowOfAgentState
import feature.agent.domain.FlowOfCurrentTask
import feature.agent.domain.FlowOfAgentTasks
import feature.agent.domain.FlowOfAgents
import feature.agent.domain.SaveAgent
import feature.agent.domain.SendAgentTask
import feature.agent.domain.StopAgentTask
import feature.agent.ui.detail.AgentDetailViewModel
import feature.agent.ui.edit.AgentEditViewModel
import feature.agent.ui.list.AgentListViewModel
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

    viewModelOf(::AgentListViewModel)
    viewModelOf(::AgentDetailViewModel)
    viewModelOf(::AgentEditViewModel)
}
