package com.example.visionai.voice

import android.util.Log
import java.text.Normalizer

enum class VoiceCommand {
    TAKE_PHOTO,
    RECORD_VIDEO,
    STOP,
    MODE_PHOTO,
    MODE_VIDEO,
    MODE_CONTINUOUS,
    REPEAT,
    DESCRIBE
}

object VoiceCommandParser {

    private data class Pattern(val phrase: String, val command: VoiceCommand)

    // Sorted longest-first so more specific patterns match before short ones.
    // Use stripped-accent forms since we normalize input.
    private val patterns: List<Pattern> = listOf(
        // TAKE_PHOTO — many natural Spanish/English variations
        Pattern("toma una foto", VoiceCommand.TAKE_PHOTO),
        Pattern("tomar una foto", VoiceCommand.TAKE_PHOTO),
        Pattern("take a picture", VoiceCommand.TAKE_PHOTO),
        Pattern("captura una foto", VoiceCommand.TAKE_PHOTO),
        Pattern("take a photo", VoiceCommand.TAKE_PHOTO),
        Pattern("take picture", VoiceCommand.TAKE_PHOTO),
        Pattern("captura foto", VoiceCommand.TAKE_PHOTO),
        Pattern("take photo", VoiceCommand.TAKE_PHOTO),
        Pattern("tomar foto", VoiceCommand.TAKE_PHOTO),
        Pattern("toma foto", VoiceCommand.TAKE_PHOTO),
        Pattern("sacar foto", VoiceCommand.TAKE_PHOTO),
        Pattern("saca foto", VoiceCommand.TAKE_PHOTO),
        Pattern("capturar", VoiceCommand.TAKE_PHOTO),
        Pattern("captura", VoiceCommand.TAKE_PHOTO),
        Pattern("foto", VoiceCommand.TAKE_PHOTO),

        // RECORD_VIDEO
        Pattern("grabar un video", VoiceCommand.RECORD_VIDEO),
        Pattern("graba un video", VoiceCommand.RECORD_VIDEO),
        Pattern("record a video", VoiceCommand.RECORD_VIDEO),
        Pattern("grabar video", VoiceCommand.RECORD_VIDEO),
        Pattern("record video", VoiceCommand.RECORD_VIDEO),
        Pattern("graba video", VoiceCommand.RECORD_VIDEO),
        Pattern("grabar", VoiceCommand.RECORD_VIDEO),

        // MODE_CONTINUOUS
        Pattern("modo continuo", VoiceCommand.MODE_CONTINUOUS),
        Pattern("continuous mode", VoiceCommand.MODE_CONTINUOUS),
        Pattern("modo automatico", VoiceCommand.MODE_CONTINUOUS),
        Pattern("continuo", VoiceCommand.MODE_CONTINUOUS),

        // MODE_PHOTO (must be after TAKE_PHOTO patterns containing "foto")
        Pattern("modo foto", VoiceCommand.MODE_PHOTO),
        Pattern("modo fotografia", VoiceCommand.MODE_PHOTO),
        Pattern("photo mode", VoiceCommand.MODE_PHOTO),

        // MODE_VIDEO (must be after RECORD_VIDEO patterns)
        Pattern("modo video", VoiceCommand.MODE_VIDEO),
        Pattern("video mode", VoiceCommand.MODE_VIDEO),

        // DESCRIBE
        Pattern("que es lo que ves", VoiceCommand.DESCRIBE),
        Pattern("what do you see", VoiceCommand.DESCRIBE),
        Pattern("que ves", VoiceCommand.DESCRIBE),
        Pattern("describir", VoiceCommand.DESCRIBE),
        Pattern("describe", VoiceCommand.DESCRIBE),
        Pattern("analiza", VoiceCommand.DESCRIBE),
        Pattern("analizar", VoiceCommand.DESCRIBE),

        // REPEAT
        Pattern("otra vez", VoiceCommand.REPEAT),
        Pattern("repetir", VoiceCommand.REPEAT),
        Pattern("repite", VoiceCommand.REPEAT),
        Pattern("repeat", VoiceCommand.REPEAT),
        Pattern("again", VoiceCommand.REPEAT),

        // STOP — avoid "para" (too common in Spanish as preposition)
        Pattern("detente", VoiceCommand.STOP),
        Pattern("detener", VoiceCommand.STOP),
        Pattern("parar", VoiceCommand.STOP),
        Pattern("pause", VoiceCommand.STOP),
        Pattern("stop", VoiceCommand.STOP),
    ).sortedByDescending { it.phrase.length }

    /** Strip accents/diacritics: "descripción" -> "descripcion" */
    private fun stripAccents(text: String): String {
        val normalized = Normalizer.normalize(text, Normalizer.Form.NFD)
        return normalized.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    fun parse(speech: String): VoiceCommand? {
        val normalized = stripAccents(speech.lowercase().trim())
        val matched = patterns.firstOrNull { normalized.contains(it.phrase) }
        Log.i("VoiceCommandParser", "Input: \"$speech\" -> normalized: \"$normalized\" -> ${matched?.command ?: "NO MATCH"}")
        return matched?.command
    }
}
