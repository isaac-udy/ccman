package feature.agent.data.storage

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

actual class ClaudeProcessStorage actual constructor() {

    private val activeProcesses = ConcurrentHashMap<String, Process>()

    init {
        Runtime.getRuntime().addShutdownHook(Thread {
            activeProcesses.values.forEach { process ->
                process.destroyForcibly()
            }
        })
    }

    actual fun startProcess(
        agentId: String,
        workingDirectory: String,
        prompt: String,
    ): Flow<String> = callbackFlow {
        val existingProcess = activeProcesses[agentId]
        if (existingProcess != null && existingProcess.isAlive) {
            existingProcess.destroyForcibly()
        }

        val command = listOf(
            "claude",
            "-p", prompt,
            "--dangerously-skip-permissions",
            "--output-format", "stream-json",
            "--verbose",
        )

        val processBuilder = ProcessBuilder(command)
            .directory(File(workingDirectory))
            .redirectErrorStream(true)

        val process = processBuilder.start()
        activeProcesses[agentId] = process

        val reader = BufferedReader(InputStreamReader(process.inputStream))

        val readJob = launch(Dispatchers.IO) {
            try {
                var line: String?
                while (isActive) {
                    line = reader.readLine() ?: break
                    trySend(line)
                }
            } catch (_: Throwable) {
                // Process ended or was destroyed
            } finally {
                reader.close()
            }
        }

        invokeOnClose {
            readJob.cancel()
            if (process.isAlive) {
                process.destroyForcibly()
            }
            activeProcesses.remove(agentId)
        }

        readJob.join()
        val exitCode = process.waitFor()
        if (exitCode != 0) {
            trySend("""{"type":"error","message":"Process exited with code $exitCode"}""")
        }

        close()
    }

    actual fun stopProcess(agentId: String) {
        val process = activeProcesses.remove(agentId)
        if (process != null && process.isAlive) {
            process.destroyForcibly()
        }
    }

    actual fun isRunning(agentId: String): Boolean {
        val process = activeProcesses[agentId]
        return process != null && process.isAlive
    }
}
