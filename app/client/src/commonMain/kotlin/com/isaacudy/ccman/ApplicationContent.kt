package com.isaacudy.ccman

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import dev.enro.NavigationKey
import dev.enro.annotations.AdvancedEnroApi
import dev.enro.backstackOf
import dev.enro.ui.NavigationDisplay
import dev.enro.ui.rememberNavigationContainer
import feature.agent.agentDependencies
import feature.agent.ui.list.AgentListDestination
import org.koin.compose.KoinApplication

@OptIn(AdvancedEnroApi::class)
@Composable
fun ApplicationContent() {
    KoinApplication(
        application = {
            modules(agentDependencies)
        }
    ) {
        MaterialTheme {
            val container = rememberNavigationContainer(
                backstack = backstackOf(NavigationKey.Instance(AgentListDestination)),
            )
            NavigationDisplay(state = container)
        }
    }
}
