package feature.agent.data.storage

expect class SlackThreadSessionStorage() {
    fun getSession(threadTs: String): SlackThreadSessionEntity?
    suspend fun saveSession(entity: SlackThreadSessionEntity)
}
