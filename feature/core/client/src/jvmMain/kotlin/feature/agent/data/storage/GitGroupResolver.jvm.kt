package feature.agent.data.storage

import java.io.File

actual fun resolveGitGroup(workingDirectory: String): String {
    return try {
        val process = ProcessBuilder("git", "remote", "get-url", "origin")
            .directory(File(workingDirectory))
            .redirectErrorStream(true)
            .start()
        val url = process.inputStream.bufferedReader().readText().trim()
        val exitCode = process.waitFor()
        if (exitCode != 0) return ""
        parseRepoIdentifier(url)
    } catch (_: Throwable) {
        ""
    }
}

private fun parseRepoIdentifier(url: String): String {
    // https://github.com/isaac-udy/Enro.git -> isaac-udy/Enro
    // git@github.com:isaac-udy/Enro.git -> isaac-udy/Enro
    val cleaned = url
        .removeSuffix(".git")
        .removeSuffix("/")

    // SSH style: git@github.com:owner/repo
    val sshMatch = Regex("""[^@]+@[^:]+:(.+)""").find(cleaned)
    if (sshMatch != null) return sshMatch.groupValues[1]

    // HTTPS style: https://github.com/owner/repo
    val httpsMatch = Regex("""https?://[^/]+/(.+)""").find(cleaned)
    if (httpsMatch != null) return httpsMatch.groupValues[1]

    return cleaned
}
