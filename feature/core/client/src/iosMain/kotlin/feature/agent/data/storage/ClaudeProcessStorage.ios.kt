package feature.agent.data.storage

import kotlinx.coroutines.flow.Flow

actual class ClaudeProcessStorage actual constructor() {
    actual fun startProcess(agentId: String, workingDirectory: String, prompt: String): Flow<String> = throw UnsupportedOperationException("Desktop only")
    actual fun stopProcess(agentId: String): Unit = throw UnsupportedOperationException("Desktop only")
    actual fun isRunning(agentId: String): Boolean = throw UnsupportedOperationException("Desktop only")
}
