package feature.agent.data.storage

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

actual class SlackThreadSessionStorage actual constructor() {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    private val sessionsDir = File(System.getProperty("user.home"), ".ccman/slack-threads").also { it.mkdirs() }

    actual fun getSession(threadTs: String): SlackThreadSessionEntity? {
        val file = File(sessionsDir, "${threadTs.hashCode()}.json")
        if (!file.exists()) return null
        return try {
            json.decodeFromString<SlackThreadSessionEntity>(file.readText())
        } catch (_: Throwable) {
            null
        }
    }

    actual suspend fun saveSession(entity: SlackThreadSessionEntity) {
        val file = File(sessionsDir, "${entity.threadTs.hashCode()}.json")
        file.writeText(json.encodeToString(entity))
    }
}
