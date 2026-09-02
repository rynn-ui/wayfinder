package com.roadguardian.app.warning

import android.content.Context
import android.media.MediaPlayer
import android.speech.tts.TextToSpeech
import android.util.Log
import com.roadguardian.app.R
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class AudioWarningManager(
    private val context: Context,
    val potholeVoiceCooldownMillis: Long = 300_000L,
    val trafficVoiceCooldownMillis: Long = 300_000L
) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "AudioWarningManager"
        private const val SYNTH_PITCH = 1.00f
        private const val SYNTH_SPEECH_RATE = 1.00f
    }

    private var tts: TextToSpeech? = null
    private var mediaPlayer: MediaPlayer? = null

    @Volatile
    private var isInitialized = false

    @Volatile
    var isEnabled: Boolean = true

    @Volatile
    var isMuted: Boolean = false
        set(value) {
            field = value
            isEnabled = !value
        }

    @Volatile
    private var isStartupPlaying = false

    @Volatile
    private var startupEndTime = 0L

    @Volatile
    private var lastPotholeAlertTime = 0L

    @Volatile
    private var lastTrafficAlertTime = 0L

    private val lastSpokenMap = ConcurrentHashMap<String, Long>()

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to instantiate TextToSpeech", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.UK)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            isInitialized = true
            applyVoiceTuning()
            Log.i(TAG, "Voice synthesizer initialized successfully")
        } else {
            Log.w(TAG, "TTS initialization failed with status $status")
        }
    }

    private fun applyVoiceTuning() {
        if (!isInitialized) return
        try {
            tts?.setPitch(SYNTH_PITCH)
            tts?.setSpeechRate(SYNTH_SPEECH_RATE)

            val availableVoices = tts?.voices
            val targetVoice = availableVoices?.firstOrNull { voice ->
                val name = voice.name.lowercase(Locale.ROOT)
                (name.contains("en-gb") || name.contains("en_gb")) &&
                    (name.contains("rjs") || name.contains("gbd") || name.contains("gba") || name.contains("female") || name.contains("fem"))
            } ?: availableVoices?.firstOrNull { voice ->
                val name = voice.name.lowercase(Locale.ROOT)
                name.contains("en-gb") || name.contains("en_gb")
            } ?: availableVoices?.firstOrNull { voice ->
                val name = voice.name.lowercase(Locale.ROOT)
                name.contains("en-us-x-sfg") || name.contains("en-us-x-iom") || name.contains("female") || name.contains("fem")
            } ?: availableVoices?.firstOrNull { it.name.lowercase(Locale.ROOT).contains("female") }
              ?: availableVoices?.firstOrNull { it.locale.language == "en" }

            if (targetVoice != null) {
                tts?.voice = targetVoice
                tts?.language = targetVoice.locale
            }
        } catch (_: Exception) {}
    }

    fun formatHazardWarning(severity: String, distanceMeters: Int): String {
        return when {
            severity.equals("critical", ignoreCase = true) ->
                "Sir. Warning: Critical crater detected $distanceMeters meters ahead. Evasive action advised."
            severity.equals("high", ignoreCase = true) ->
                "Sir. High impact pothole in $distanceMeters meters. Decelerate now."
            else ->
                "Sir. Road anomaly detected $distanceMeters meters ahead."
        }
    }

    fun getTestPhrase(): String {
        return "Monitoring mode active."
    }

    fun playStartupSound(onComplete: (() -> Unit)? = null) {
        if (isMuted || !isEnabled) {
            onComplete?.invoke()
            return
        }
        isStartupPlaying = true
        startupEndTime = System.currentTimeMillis() + 3500L
        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context.applicationContext, R.raw.system_startup)?.apply {
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    isStartupPlaying = false
                    onComplete?.invoke()
                }
                setOnErrorListener { _, _, _ ->
                    mediaPlayer = null
                    isStartupPlaying = false
                    onComplete?.invoke()
                    true
                }
                start()
            }
            if (mediaPlayer == null) {
                isStartupPlaying = false
                onComplete?.invoke()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to play startup audio", e)
            isStartupPlaying = false
            onComplete?.invoke()
        }
    }

    fun speakAnnouncement(text: String, onComplete: (() -> Unit)? = null) {
        if (!isEnabled || isMuted || !isInitialized || text.isBlank()) {
            onComplete?.invoke()
            return
        }
        try {
            applyVoiceTuning()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "announcement_${System.currentTimeMillis()}")
            onComplete?.invoke()
        } catch (e: Exception) {
            Log.w(TAG, "TTS announcement failed", e)
            onComplete?.invoke()
        }
    }

    fun warnHazard(severity: String, distanceMeters: Int, hazardId: String? = null, force: Boolean = false) {
        if (!isEnabled || isMuted || isStartupPlaying || System.currentTimeMillis() < startupEndTime) return

        val now = System.currentTimeMillis()
        if (!force) {
            if (now - lastPotholeAlertTime < potholeVoiceCooldownMillis) {
                return
            }
            if (hazardId != null) {
                val last = lastSpokenMap[hazardId] ?: 0L
                if (now - last < potholeVoiceCooldownMillis) {
                    return
                }
                lastSpokenMap[hazardId] = now
            }
        }
        lastPotholeAlertTime = now

        val rawResId = when {
            severity.equals("critical", ignoreCase = true) -> when {
                distanceMeters <= 35 -> R.raw.warning_critical_30
                distanceMeters <= 60 -> R.raw.warning_critical_50
                else -> R.raw.warning_critical_75
            }
            severity.equals("high", ignoreCase = true) -> when {
                distanceMeters <= 35 -> R.raw.warning_high_30
                distanceMeters <= 60 -> R.raw.warning_high_50
                else -> R.raw.warning_high_75
            }
            else -> when {
                distanceMeters <= 35 -> R.raw.warning_anomaly_30
                distanceMeters <= 60 -> R.raw.warning_anomaly_50
                else -> R.raw.warning_anomaly_75
            }
        }

        try {
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(context.applicationContext, rawResId)?.apply {
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                }
                start()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Audio file playback failed, fallback to TTS", e)
            speak(formatHazardWarning(severity, distanceMeters), hazardId, force)
        }
    }

    fun warnTraffic(
        type: String,
        streetName: String,
        severity: String,
        distanceMeters: Int,
        incidentId: String? = null
    ) {
        if (!isEnabled || isMuted || !isInitialized || isStartupPlaying || System.currentTimeMillis() < startupEndTime) return

        val now = System.currentTimeMillis()
        if (now - lastTrafficAlertTime < trafficVoiceCooldownMillis) {
            return
        }
        if (incidentId != null) {
            val last = lastSpokenMap[incidentId] ?: 0L
            if (now - last < trafficVoiceCooldownMillis) {
                return
            }
            lastSpokenMap[incidentId] = now
        }
        lastTrafficAlertTime = now

        val streetText = if (streetName.isNotBlank()) " on $streetName" else ""
        val distText = if (distanceMeters >= 1000) "${distanceMeters / 1000} kilometers" else "${((distanceMeters + 25) / 50) * 50} meters"
        val speech = when {
            severity.contains("STANDSTILL", ignoreCase = true) || severity.contains("HEAVY", ignoreCase = true) ->
                "Caution: Heavy traffic congestion ahead$streetText, approximately $distText away."
            type.contains("ACCIDENT", ignoreCase = true) ->
                "Warning: Accident reported ahead$streetText, approximately $distText away."
            type.contains("ROAD_CLOSED", ignoreCase = true) ->
                "Warning: Road closed ahead$streetText."
            else ->
                "Notice: Traffic slowdown ahead$streetText, approximately $distText away."
        }

        try {
            applyVoiceTuning()
            tts?.speak(speech, TextToSpeech.QUEUE_FLUSH, null, incidentId ?: "traffic_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.w(TAG, "Traffic TTS warning failed", e)
        }
    }

    fun speak(text: String, hazardId: String? = null, force: Boolean = false) {
        if (!isEnabled || isMuted || !isInitialized || text.isBlank() || isStartupPlaying || System.currentTimeMillis() < startupEndTime) return

        val now = System.currentTimeMillis()
        if (!force) {
            if (now - lastPotholeAlertTime < potholeVoiceCooldownMillis) {
                return
            }
            if (hazardId != null) {
                val last = lastSpokenMap[hazardId] ?: 0L
                if (now - last < potholeVoiceCooldownMillis) {
                    return
                }
                lastSpokenMap[hazardId] = now
            }
        }
        lastPotholeAlertTime = now

        try {
            applyVoiceTuning()
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, hazardId ?: "warning_${System.currentTimeMillis()}")
        } catch (e: Exception) {
            Log.w(TAG, "TTS speak failed", e)
        }
    }

    fun resetCooldown() {
        lastPotholeAlertTime = 0L
        lastTrafficAlertTime = 0L
        lastSpokenMap.clear()
    }

    fun stop() {
        try {
            isStartupPlaying = false
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            tts?.stop()
        } catch (_: Exception) {}
    }

    fun release() {
        try {
            isStartupPlaying = false
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            tts?.stop()
            tts?.shutdown()
            tts = null
            isInitialized = false
        } catch (_: Exception) {}
    }
}
