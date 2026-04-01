package architecture

import com.lemonappdev.konsist.api.declaration.KoBaseDeclaration
import com.lemonappdev.konsist.api.declaration.KoClassDeclaration
import com.lemonappdev.konsist.api.declaration.KoFunctionDeclaration

object ArchitectureExceptions {
    val classes = listOf(
        "feature.agent.data.SlackRepository",
    )

    val functions = listOf(
        "feature.agent.data.storage.resolveGitGroup",
        "feature.agent.data.storage.deleteDirectory",
    )

    fun isIgnored(declaration: KoBaseDeclaration): Boolean {
        return when (declaration) {
            is KoClassDeclaration -> declaration.fullyQualifiedName in classes
            is KoFunctionDeclaration -> declaration.fullyQualifiedName in functions
            else -> false
        }
    }
}
