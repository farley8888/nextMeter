package com.vismo.cablemeter.util

import android.content.Context
import android.content.res.Resources
import android.media.MediaPlayer
import android.util.Log
import com.vismo.cablemeter.R
import com.vismo.cablemeter.datastore.TripDataStore
import com.vismo.cablemeter.model.TripStatus
import com.vismo.cablemeter.module.IoDispatcher
import com.vismo.cablemeter.ui.meter.TtsLanguagePref
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import java.io.IOException
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.roundToInt

@Singleton
class TtsUtil @Inject constructor(
    private val appContext: Context,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val localeHelper: LocaleHelper
) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentFileIndex = 0
    private var audioFiles = listOf<Int>()

    // English setup
    private val belowTwenty = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19)
    private val tens = listOf(0, 0, 20, 30, 40, 50, 60, 70, 80, 90)

    // Chinese setup
    private val numbersCN = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9)
    private val positionCN = listOf(0, 10, 100, 1000) // 0 is not used

    private val scope = CoroutineScope(ioDispatcher + SupervisorJob())

    private val _currentLanguagePref = MutableStateFlow<TtsLanguagePref>(TtsLanguagePref.OFF)

    private val _wasTripJustStarted = MutableStateFlow(false)
    private val _wasTripJustPaused = MutableStateFlow(false)

    fun setLanguagePref(pref: TtsLanguagePref) {
        _currentLanguagePref.value = pref
    }

    fun setWasTripJustStarted(value: Boolean) {
        _wasTripJustStarted.value = value
    }

    fun setWasTripJustPaused(value: Boolean) {
        _wasTripJustPaused.value = value
    }

    init {
        scope.launch {
            TripDataStore.tripData.collect { trip ->
                trip?.let {
                    when (trip.tripStatus) {
                        TripStatus.HIRED -> {
                            if (_currentLanguagePref.value != TtsLanguagePref.OFF && _wasTripJustStarted.value && trip.licensePlate.isNotBlank()) {
                                playWhenTripStarted(trip.licensePlate)
                                setWasTripJustStarted(false)
                            }
                        }

                        TripStatus.STOP -> {
                            if (_currentLanguagePref.value != TtsLanguagePref.OFF && _wasTripJustPaused.value) {
                                playWhenTripPaused(trip.totalFare, trip.extra > 0)
                                setWasTripJustPaused(false)
                            }
                        }

                        else -> {

                        }
                    }
                }
            }
        }
    }


    private fun playWhenTripStarted(licensePlate: String) {
        stopAndReleaseMediaPlayer()
        currentFileIndex = 0

        val startTripAudioFiles = mutableListOf(R.raw.start)
        val letters = licensePlate.toCharArray() // Improved string splitting

        letters.forEach { letter ->
            val mp3FileId = getResourceIdFromName(appContext, getFileName(letter.toString()), appContext.packageName)
            if (mp3FileId > 0) startTripAudioFiles.add(mp3FileId)
        }

        startTripAudioFiles.add(R.raw.start_closing)
        audioFiles = startTripAudioFiles

        scope.launch {
            playNextAudio(appContext)
        }
    }

    private fun playNextAudio(context: Context) {
        if (currentFileIndex >= audioFiles.size) {
            currentFileIndex = 0
            return
        }

        val afd = try {
            context.resources.openRawResourceFd(audioFiles[currentFileIndex])
        } catch (e: Resources.NotFoundException) {
            null
        }

        afd?.let { descriptor ->
            mediaPlayer?.reset() ?: run { mediaPlayer = MediaPlayer() }
            try {
                mediaPlayer?.apply {
                    setDataSource(descriptor.fileDescriptor, descriptor.startOffset, 3000L.coerceAtLeast(descriptor.length))
                    prepareAsync()
                    setOnPreparedListener {
                        start()
                    }
                    setOnCompletionListener {
                        currentFileIndex++
                        playNextAudio(context)
                    }
                }
                descriptor.close()
            } catch (e: IOException) {
                Log.e("TTS", "Error playing audio file [${audioFiles[currentFileIndex]}]", e)
            }
        } ?: run {
            Log.e("TTS", "FileNotFound: ${audioFiles[currentFileIndex]}")
        }
    }

    private fun playWhenTripPaused(
        tripAmount: Double,
        includedSurcharged: Boolean,
    ) {
        val locale = localeHelper.getLocale()
        stopAndReleaseMediaPlayer()
        currentFileIndex = 0

        val (integerList, decimalList) = mapToNumbersArray(tripAmount, locale)
        val amountAudioFiles = mutableListOf<Int>()

        integerList.forEach { integerItem ->
            val mp3FileId = getResourceIdFromName(appContext, getFileName(integerItem.toString()), appContext.packageName)
            if (mp3FileId > 0) amountAudioFiles.add(mp3FileId)
        }

        if (locale == Locale.ENGLISH) amountAudioFiles.add(R.raw.dollar)
        if (decimalList.isNotEmpty()) {
            amountAudioFiles.add(if (locale == Locale.ENGLISH) R.raw.and else R.raw.point)
        }

        decimalList.forEach { decimalItem ->
            val mp3FileId = getResourceIdFromName(appContext, getFileName(decimalItem.toString()), appContext.packageName)
            if (mp3FileId > 0) amountAudioFiles.add(mp3FileId)
        }

        if (decimalList.isNotEmpty() && locale == Locale.ENGLISH) {
            amountAudioFiles.add(R.raw.cents)
        } else if (locale != Locale.ENGLISH) {
            amountAudioFiles.add(R.raw.dollar)
        }

        audioFiles = if (appContext.resources.configuration.locale.language == "zh-rhk" && includedSurcharged) {
            currentFileIndex = 1
            listOf(R.raw.end_before, R.raw.end_after_with_surcharge) + amountAudioFiles + R.raw.end_after
        } else {
            currentFileIndex = 0
            listOf(R.raw.end_before) + amountAudioFiles + if (includedSurcharged) R.raw.end_after_with_surcharge else R.raw.end_after
        }
        scope.launch {
            playNextAudio(appContext)
        }
    }

    // Helper function for safe media player stopping and releasing
    private fun stopAndReleaseMediaPlayer() {
        mediaPlayer?.apply {
            stop()
            reset()
        }
    }

    // Get resource ID
    private fun getResourceIdFromName(context: Context, resourceName: String, packageName: String): Int {
        return context.resources.getIdentifier(resourceName, "raw", packageName).takeIf { it != 0 } ?: -1
    }

    // Converts a license plate character or number into the corresponding file name
    private fun getFileName(value: String): String {
        val isInt = value.toIntOrNull()
        return if (isInt != null) {
            "number_$value"
        } else {
            "letter_${value.lowercase(Locale.getDefault())}"
        }
    }

    // Mapping numbers to an array based on locale
    private fun mapToNumbersArray(number: Double, locale: Locale): Pair<List<Int>, List<Int>> {
        return if (locale == Locale.ENGLISH) {
            mapToNumbersArrayEn(number)
        } else {
            numberToWordsCh(number)
        }
    }

    private fun mapToNumbersArrayEn(number: Double): Pair<List<Int>, List<Int>> {
        fun convertBelowTwenty(num: Int): List<Int> = listOf(belowTwenty[num])

        fun convertIntegerPart(num: Int): List<Int> {
            return when {
                num < 20 -> convertBelowTwenty(num)
                num < 100 -> listOf(tens[num / 10]) + if (num % 10 > 0) convertBelowTwenty(num % 10) else emptyList()
                num < 1000 -> listOf(belowTwenty[num / 100], 100) + if (num % 100 > 0) convertIntegerPart(num % 100) else emptyList()
                else -> convertIntegerPart(num / 1000) + listOf(1000) + if (num % 1000 > 0) convertIntegerPart(num % 1000) else emptyList()
            }
        }

        fun convertDecimalPart(num: Int): List<Int> {
            return if (num > 0) convertIntegerPart(num) else emptyList()
        }

        val intPart = number.toInt()
        val decimalPart = ((number - intPart) * 100).roundToInt()

        return Pair(convertIntegerPart(intPart), convertDecimalPart(decimalPart))
    }

    private fun numberToWordsCh(number: Double): Pair<List<Int>, List<Int>> {
        fun convertIntegerPart(num: Int): List<Int> {
            var result = mutableListOf<Int>()
            var tempNum = num
            var position = 0

            while (tempNum > 0) {
                val digit = tempNum % 10
                if (digit > 0) {
                    if (position > 0) result.add(0, positionCN[position])
                    result.add(0, numbersCN[digit])
                } else if (result.isNotEmpty() && result.first() != numbersCN[0]) {
                    result.add(0, numbersCN[0])
                }
                tempNum /= 10
                position++
            }
            return result
        }

        fun convertDecimalPart(num: Int): List<Int> {
            if (num <= 0) return emptyList()

            val result = mutableListOf<Int>()
            val numStr = num.toString()

            if (num < 10) {
                result.add(numbersCN[0]) // Leading zero for decimals like 0.X
            }

            numStr.forEach { digit ->
                val intDigit = digit.digitToInt()
                if (intDigit > 0) {
                    result.add(numbersCN[intDigit])
                }
            }
            return result
        }

        val intPart = number.toInt()
        val decimalPart = ((number - intPart) * 100).roundToInt()

        return Pair(convertIntegerPart(intPart), convertDecimalPart(decimalPart))
    }

    fun isPlaying(): Boolean {
        return (mediaPlayer?.isPlaying == true)
    }
}
