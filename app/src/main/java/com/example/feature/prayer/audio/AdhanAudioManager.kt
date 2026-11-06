package com.example.feature.prayer.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.example.R
import com.example.feature.prayer.domain.model.AudioSourceType

class AdhanAudioManager(private val context: Context) {

    private val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private var mediaPlayer: MediaPlayer? = null
    private var focusRequest: AudioFocusRequest? = null
    private val handler = Handler(Looper.getMainLooper())
    private var stopRunnable: Runnable? = null

    private var onPlaybackFinishedCallback: (() -> Unit)? = null

    fun playAdhan(
        audioSource: AudioSourceType,
        customUriString: String?,
        durationSeconds: Int = 30,
        onComplete: (() -> Unit)? = null
    ): Boolean {
        stopAdhan()
        onPlaybackFinishedCallback = onComplete

        // Check silent/DND mode: do not play sound if phone is on silent mode
        val ringerMode = audioManager.ringerMode
        if (ringerMode == AudioManager.RINGER_MODE_SILENT) {
            onComplete?.invoke()
            return false
        }

        // Request Audio Focus
        val focusGranted = requestAudioFocus()
        if (!focusGranted) {
            onComplete?.invoke()
            return false
        }

        try {
            val player = MediaPlayer()
            val audioAttributes = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            player.setAudioAttributes(audioAttributes)

            var dataSourceSet = false

            // Try custom audio first if requested
            if (audioSource == AudioSourceType.CUSTOM && !customUriString.isNullOrBlank()) {
                try {
                    val uri = Uri.parse(customUriString)
                    context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                        player.setDataSource(pfd.fileDescriptor)
                        dataSourceSet = true
                    }
                } catch (e: Exception) {
                    dataSourceSet = false // Fallback to bundled
                }
            }

            // Fallback or default to bundled Sheikh Rabah Ben Drah Algerian Adhan
            if (!dataSourceSet) {
                val afd = context.resources.openRawResourceFd(R.raw.adhan_rabah)
                player.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                afd.close()
            }

            player.prepare()
            player.setOnCompletionListener {
                stopAdhan()
            }
            player.setOnErrorListener { _, _, _ ->
                stopAdhan()
                true
            }

            player.start()
            mediaPlayer = player

            // Configured duration timer (e.g. 30s)
            val durationMs = (durationSeconds.coerceIn(10, 180) * 1000L)
            stopRunnable = Runnable {
                stopAdhan()
            }
            handler.postDelayed(stopRunnable!!, durationMs)

            return true
        } catch (e: Exception) {
            stopAdhan()
            return false
        }
    }

    fun stopAdhan() {
        stopRunnable?.let { handler.removeCallbacks(it) }
        stopRunnable = null

        try {
            mediaPlayer?.let { player ->
                if (player.isPlaying) {
                    player.stop()
                }
                player.reset()
                player.release()
            }
        } catch (_: Exception) {
        } finally {
            mediaPlayer = null
        }

        abandonAudioFocus()

        val callback = onPlaybackFinishedCallback
        onPlaybackFinishedCallback = null
        callback?.invoke()
    }

    fun isPlaying(): Boolean {
        return try {
            mediaPlayer?.isPlaying == true
        } catch (_: Exception) {
            false
        }
    }

    private fun requestAudioFocus(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val attr = AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setAudioAttributes(attr)
                .setOnAudioFocusChangeListener { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS ||
                        focusChange == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT
                    ) {
                        stopAdhan()
                    }
                }
                .build()
            focusRequest = request
            audioManager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        } else {
            @Suppress("DEPRECATION")
            audioManager.requestAudioFocus(
                { focusChange ->
                    if (focusChange == AudioManager.AUDIOFOCUS_LOSS) {
                        stopAdhan()
                    }
                },
                AudioManager.STREAM_ALARM,
                AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
            ) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        }
    }

    private fun abandonAudioFocus() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
            focusRequest = null
        } else {
            @Suppress("DEPRECATION")
            audioManager.abandonAudioFocus(null)
        }
    }
}
