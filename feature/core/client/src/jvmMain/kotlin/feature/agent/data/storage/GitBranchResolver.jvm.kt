package feature.agent.data.storage

import java.io.File

actual fun resolveGitBranch(workingDirectory: String): String {
    return try {
        val process = ProcessBuilder("git", "branch", "--show-current")
            .directory(File(workingDirectory))
            .redirectErrorStream(true)
            .start()
        val branch = process.inputStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()
        if (exitCode != 0) "" else branch
    } catch (_: Throwable) {
        ""
    }
}
