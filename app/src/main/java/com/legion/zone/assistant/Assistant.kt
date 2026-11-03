package com.legion.zone.assistant

import android.content.Context
import com.legion.zone.perf.PerfController
import com.legion.zone.perf.Profile
import com.legion.zone.thermal.Level
import com.legion.zone.thermal.ThermalBus

/** The six expressions from the Live2D strip in the design. Drive the avatar layer with these. */
enum class Emotion { NORMAL, HAPPY, ANGRY, SURPRISED, SHY, GREETING }

data class ToolCall(val name: String, val args: Map<String, String> = emptyMap())
data class AssistantReply(val text: String, val emotion: Emotion, val tool: ToolCall? = null)

/** Plug your LLM here (through YOUR backend proxy — never ship an API key inside the APK). */
fun interface LlmClient { suspend fun complete(system: String, user: String): String }

object AssistantEngine {
    var llm: LlmClient? = null

    const val SYSTEM_PROMPT = """You are the Legion Zone gaming assistant. Reply briefly.
Tools you may request as JSON: set_profile(COOL|BALANCED|MAX), get_temperature, kill_background.
Always end with [emotion: NORMAL|HAPPY|ANGRY|SURPRISED|SHY|GREETING]."""

    suspend fun reply(ctx: Context, input: String): AssistantReply {
        llm?.let { return AssistantReply(it.complete(SYSTEM_PROMPT, input), Emotion.NORMAL) }

        val t = input.lowercase()
        val th = ThermalBus.state.value
        fun has(vararg k: String) = k.any { t.contains(it) }
        return when {
            has("hot", "temp", "heat", "سخن", "حرار") -> AssistantReply(
                "Thermal level ${th.level.name}, battery ${"%.1f".format(th.battTempC)}°C.",
                if (th.level >= Level.HOT) Emotion.ANGRY else Emotion.HAPPY,
                ToolCall("get_temperature"))
            has("cool", "برد") -> {
                PerfController.apply(ctx, Profile.COOL)
                AssistantReply("Cool profile on. Refresh capped at 60 Hz.", Emotion.HAPPY,
                    ToolCall("set_profile", mapOf("profile" to "COOL")))
            }
            has("max", "performance", "أداء", "اقصى", "أقصى") -> {
                PerfController.apply(ctx, Profile.MAX)
                AssistantReply("Max profile on. I'll watch the heat.", Emotion.SURPRISED,
                    ToolCall("set_profile", mapOf("profile" to "MAX")))
            }
            has("hello", "hi", "hey", "مرحبا", "اهلا", "أهلا") -> AssistantReply("Welcome back.", Emotion.GREETING)
            else -> AssistantReply("No LLM connected yet. Set AssistantEngine.llm to enable open chat.", Emotion.NORMAL)
        }
    }
}
