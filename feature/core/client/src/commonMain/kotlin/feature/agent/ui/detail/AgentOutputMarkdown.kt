package feature.agent.ui.detail

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownColor
import com.mikepenz.markdown.m3.markdownTypography

@Composable
internal fun AgentOutputMarkdown(
    content: String,
    modifier: Modifier = Modifier,
) {
    val baseText = MaterialTheme.typography.bodySmall
    val typography = markdownTypography(
        h1 = MaterialTheme.typography.titleMedium,
        h2 = MaterialTheme.typography.titleSmall,
        h3 = MaterialTheme.typography.labelLarge,
        h4 = MaterialTheme.typography.labelMedium,
        h5 = MaterialTheme.typography.labelSmall,
        h6 = MaterialTheme.typography.labelSmall,
        text = baseText,
        code = baseText.copy(fontFamily = FontFamily.Monospace),
        inlineCode = baseText.copy(fontFamily = FontFamily.Monospace),
        quote = baseText.copy(fontStyle = FontStyle.Italic),
        paragraph = baseText,
        ordered = baseText,
        bullet = baseText,
        list = baseText,
        textLink = TextLinkStyles(
            style = baseText.copy(
                fontWeight = FontWeight.Bold,
                textDecoration = TextDecoration.Underline,
            ).toSpanStyle()
        ),
        table = baseText,
    )

    Markdown(
        content = content,
        typography = typography,
        colors = markdownColor(),
        modifier = modifier,
    )
}
