package feature.agent.data.storage

import java.io.File

actual fun deleteDirectory(path: String): Boolean {
    val dir = File(path)
    if (!dir.exists()) return true
    return dir.deleteRecursively()
}
