package feature.agent.data.storage

import com.slack.api.Slack
import com.slack.api.bolt.App
import com.slack.api.bolt.AppConfig
import com.slack.api.bolt.socket_mode.SocketModeApp
import com.slack.api.model.event.AppMentionEvent
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import java.util.concurrent.atomic.AtomicReference

actual class SlackServiceStorage actual constructor() {

    private val slack = Slack.getInstance()
    private val socketModeApp = AtomicReference<SocketModeApp?>(null)
    private var botToken: String? = null

    actual fun connect(botToken: String, appToken: String): Flow<SlackMessageEntity> = callbackFlow {
        this@SlackServiceStorage.botToken = botToken

        val appConfig = AppConfig.builder()
            .singleTeamBotToken(botToken)
            .build()
        val app = App(appConfig)

        app.event(AppMentionEvent::class.java) { payload, ctx ->
            val event = payload.event
            val text = event.text
                .replace(Regex("<@[A-Z0-9]+>\\s*"), "")
                .trim()

            if (text.isNotBlank()) {
                trySend(
                    SlackMessageEntity(
                        channelId = event.channel,
                        threadTs = event.threadTs,
                        messageTs = event.ts,
                        text = text,
                        userId = event.user,
                    )
                )
            }
            ctx.ack()
        }

        val sma = SocketModeApp(appToken, app)
        socketModeApp.set(sma)

        sma.startAsync()

        awaitClose {
            try {
                sma.close()
            } catch (_: Throwable) {
            }
            socketModeApp.set(null)
        }
    }

    actual fun disconnect() {
        try {
            socketModeApp.getAndSet(null)?.close()
        } catch (_: Throwable) {
        }
    }

    actual fun isConnected(): Boolean = socketModeApp.get() != null

    actual suspend fun postMessage(channelId: String, text: String, threadTs: String?): String {
        val token = botToken ?: error("Not connected")
        val response = slack.methods(token).chatPostMessage { req ->
            req.channel(channelId)
                .text(text)
                .also { if (threadTs != null) it.threadTs(threadTs) }
        }
        if (!response.isOk) {
            error("Failed to post message: ${response.error}")
        }
        return response.ts
    }

    actual suspend fun updateMessage(channelId: String, ts: String, text: String) {
        val token = botToken ?: error("Not connected")
        val response = slack.methods(token).chatUpdate { req ->
            req.channel(channelId)
                .ts(ts)
                .text(text)
        }
        if (!response.isOk) {
            error("Failed to update message: ${response.error}")
        }
    }

    actual suspend fun listChannels(): List<SlackChannelEntity> {
        val token = botToken ?: error("Not connected")
        val response = slack.methods(token).conversationsList { req ->
            req.excludeArchived(true)
                .limit(1000)
        }
        if (!response.isOk) {
            error("Failed to list channels: ${response.error}")
        }
        return response.channels.map { channel ->
            SlackChannelEntity(id = channel.id, name = channel.name)
        }
    }

    actual suspend fun createChannel(name: String): SlackChannelEntity {
        val token = botToken ?: error("Not connected")
        val response = slack.methods(token).conversationsCreate { req ->
            req.name(name)
        }
        if (!response.isOk) {
            error("Failed to create channel: ${response.error}")
        }
        return SlackChannelEntity(
            id = response.channel.id,
            name = response.channel.name,
        )
    }
}
