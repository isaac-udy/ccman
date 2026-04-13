package feature.agent.data.storage

import kotlinx.coroutines.flow.Flow

expect class ClaudeProcessStorage() {
    fun startProcess(agentId: String, workingDirectory: String, prompt: String, sessionId: String?): Flow<String>
    fun stopProcess(agentId: String)
    fun isRunning(agentId: String): Boolean
}
