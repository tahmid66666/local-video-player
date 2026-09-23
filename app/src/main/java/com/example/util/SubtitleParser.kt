package com.example.util

import android.content.Context
import android.net.Uri
import com.example.R
import java.io.BufferedReader
import java.io.InputStreamReader

data class SubtitleCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

object SubtitleParser {

    fun parseSrt(content: String): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val lines = content.replace("\r\n", "\n").replace("\r", "\n").split("\n")
        var i = 0
        while (i < lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty() || line.all { it.isDigit() }) {
                i++
                continue
            }

            if (line.contains("-->")) {
                val times = line.split("-->")
                if (times.size == 2) {
                    val startMs = parseTimestamp(times[0].trim())
                    val endMs = parseTimestamp(times[1].trim())

                    i++
                    val textBuilder = StringBuilder()
                    while (i < lines.size && lines[i].trim().isNotEmpty()) {
                        if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                        textBuilder.append(lines[i].trim())
                        i++
                    }

                    if (startMs >= 0 && endMs > startMs) {
                        cues.add(SubtitleCue(startMs, endMs, textBuilder.toString()))
                    }
                }
            }
            i++
        }
        return cues
    }

    private fun parseTimestamp(timeString: String): Long {
        return try {
            // Format: 00:01:23,456 or 01:23.456
            val cleaned = timeString.replace(',', '.')
            val parts = cleaned.split(":")
            if (parts.size == 3) {
                val hours = parts[0].trim().toLong()
                val minutes = parts[1].trim().toLong()
                val secondsParts = parts[2].trim().split(".")
                val seconds = secondsParts[0].toLong()
                val millis = if (secondsParts.size > 1) {
                    secondsParts[1].padEnd(3, '0').take(3).toLong()
                } else 0L
                (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
            } else if (parts.size == 2) {
                val minutes = parts[0].trim().toLong()
                val secondsParts = parts[1].trim().split(".")
                val seconds = secondsParts[0].toLong()
                val millis = if (secondsParts.size > 1) {
                    secondsParts[1].padEnd(3, '0').take(3).toLong()
                } else 0L
                (minutes * 60 + seconds) * 1000 + millis
            } else {
                -1L
            }
        } catch (e: Exception) {
            -1L
        }
    }

    fun loadSampleSubtitle(context: Context, resId: Int): List<SubtitleCue> {
        return try {
            val inputStream = context.resources.openRawResource(resId)
            val reader = BufferedReader(InputStreamReader(inputStream))
            val text = reader.readText()
            reader.close()
            parseSrt(text)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun loadSubtitleFromUri(context: Context, uri: Uri): List<SubtitleCue> {
        return try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val reader = BufferedReader(InputStreamReader(stream))
                val text = reader.readText()
                parseSrt(text)
            } ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun getActiveSubtitle(cues: List<SubtitleCue>, currentMs: Long): String? {
        return cues.firstOrNull { currentMs in it.startTimeMs..it.endTimeMs }?.text
    }
}
