package feature.agent.data.storage

import java.io.File

actual fun gitCheckout(workingDirectory: String, branch: String): Boolean {
    return try {
        val process = ProcessBuilder("git", "checkout", branch)
            .directory(File(workingDirectory))
            .redirectErrorStream(true)
            .start()
        process.inputStream.bufferedReader().readText()
        process.waitFor() == 0
    } catch (_: Throwable) {
        false
    }
}
