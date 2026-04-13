package feature.agent.data.storage

actual class SlackThreadSessionStorage actual constructor() {
    actual fun getSession(threadTs: String): SlackThreadSessionEntity? = throw UnsupportedOperationException("Desktop only")
    actual suspend fun saveSession(entity: SlackThreadSessionEntity): Unit = throw UnsupportedOperationException("Desktop only")
}
