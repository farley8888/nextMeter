package com.vismo.nextgenmeter.ui.settings

import android.content.Context
import android.media.AudioManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ilin.util.ShellUtils
import com.vismo.nextgenmeter.module.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class AdjustBrightnessOrVolumeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _brightnessLevel = MutableStateFlow(0.5f) // Default brightness level (0.0f - 1.0f)
    val brightnessLevel: StateFlow<Float> = _brightnessLevel

    private val _volumeLevel = MutableStateFlow(0.5f) // Default volume level (0.0f - 1.0f)
    val volumeLevel: StateFlow<Float> = _volumeLevel


    init {
        fetchCurrentSettings()
    }

    fun updateBrightness(level: Float) {
        viewModelScope.launch(ioDispatcher) {
            if(level > 0.1F) { // don't allow brightness to fully be 0
                _brightnessLevel.value = level
                val newBrightness = (level * 255).toInt()
                ShellUtils.execEcho("echo $newBrightness > /sys/class/backlight/backlight/brightness")
            }
        }
    }

    fun updateVolume(level: Float) {
        _volumeLevel.value = level
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val newVolume = (level * audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)).toInt()
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
    }

    private fun fetchCurrentSettings() {
        viewModelScope.launch(ioDispatcher) {
            val brightness = getSystemBrightness()
            _brightnessLevel.value = brightness

            val volume = getSystemVolume()
            _volumeLevel.value = volume
        }
    }

    private suspend fun getSystemBrightness(): Float {
        return withContext(Dispatchers.IO) {
            try {
                // Assuming the command returns brightness between 0 and a maximum value (e.g., 255)
                val brightnessVal = ShellUtils.execShellCmd("cat /sys/class/backlight/backlight/brightness").trim()
                val maxBrightness = 255f
                val brightnessInt = brightnessVal.toIntOrNull() ?: 127 // Default to 50% if parsing fails
                brightnessInt / maxBrightness
            } catch (e: Exception) {
                0.5f // Default to 50% on error
            }
        }
    }

    private fun getSystemVolume(): Float {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        return currentVolume / maxVolume.toFloat()
    }
}