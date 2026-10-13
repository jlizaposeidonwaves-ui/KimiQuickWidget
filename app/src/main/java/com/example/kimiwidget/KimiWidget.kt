package com.example.kimiwidget

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.glance.Button
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.ActionParameters
import androidx.glance.appwidget.action.actionParametersOf
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.dp
import androidx.glance.unit.sp
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class KimiWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = KimiWidget()
}

class KimiWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            GlanceTheme {
                WidgetContent()
            }
        }
    }

    companion object {
        const val KIMI_PACKAGE = "com.moonshot.kimi"
        const val KIMI_WEB = "https://www.kimi.com/"

        val SHORTCUTS = listOf(
            "Summarize" to "Summarize the key points of my last conversation",
            "Translate" to "Translate 'where is the nearest coffee shop?' into Japanese and Korean",
            "Ideas" to "Give me 5 creative ideas for a weekend project",
            "Fact" to "Explain one interesting science fact in two sentences",
            "Reply" to "Draft a short friendly reply to this message: "
        )
    }
}

@Composable
private fun WidgetContent() {
    val now = Date()
    val fmt = SimpleDateFormat("HH:mm", Locale.getDefault())
    val hour = SimpleDateFormat("HH", Locale.getDefault()).format(now).toInt()
    val greeting = when {
        hour < 12 -> "Good morning"
        hour < 18 -> "Good afternoon"
        else -> "Good evening"
    }

    Column(modifier = GlanceModifier.fillMaxSize().padding(12.dp)) {
        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Kimi", style = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium))
            Text(
                text = fmt.format(now),
                modifier = GlanceModifier.defaultWeight().padding(start = 8.dp),
                style = TextStyle(fontSize = 12.sp)
            )
        }

        Text(text = greeting, style = TextStyle(fontSize = 13.sp))

        KimiWidget.SHORTCUTS.chunked(2).forEach { rowItems ->
            Row(modifier = GlanceModifier.fillMaxWidth().padding(top = 8.dp)) {
                rowItems.forEach { (label, prompt) ->
                    Button(
                        text = label,
                        onClick = {
                            actionRunCallback<SendPrompt>(
                                actionParametersOf(SendPrompt.PromptKey to prompt)
                            )
                        }
                    )
                }
            }
        }
    }
}

class SendPrompt : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prompt = parameters[PromptKey]?.trim().orEmpty()
        if (prompt.isEmpty()) return
        handoffToKimi(context, prompt)
    }

    companion object {
        val PromptKey = ActionParameters.Key<String>("prompt")
    }
}

private fun handoffToKimi(context: Context, prompt: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, prompt)
        setPackage(KimiWidget.KIMI_PACKAGE)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(send)
    } catch (e: ActivityNotFoundException) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(KimiWidget.KIMI_WEB))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
