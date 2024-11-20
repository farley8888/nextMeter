package com.vismo.nextgenmeter.util

import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import android.util.Base64
import android.view.HapticFeedbackConstants
import android.view.View
import com.google.firebase.Timestamp
import com.vismo.nextgenmeter.util.Constant.SLAT_KEY
import com.vismo.nextgenmeter.util.Constant.VECTOR_KEY
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.crypto.Cipher
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

object GlobalUtils {
    fun loadPublicKey(pem: String): RSAPublicKey {
        val pemCleaned = pem
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s+".toRegex(), "")
        val encoded = Base64.decode(pemCleaned, Base64.DEFAULT)
        val keySpec = X509EncodedKeySpec(encoded)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePublic(keySpec) as RSAPublicKey
    }

    fun loadPrivateKey(pem: String): RSAPrivateKey {
        val clear = pem
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("\\s+".toRegex(), "")
        val encoded = Base64.decode(clear, Base64.DEFAULT)
        val keySpec = PKCS8EncodedKeySpec(encoded)
        val keyFactory = KeyFactory.getInstance("RSA")
        return keyFactory.generatePrivate(keySpec) as RSAPrivateKey
    }

    fun String.divideBy100AndConvertToDouble(): Double {
        return BigDecimal(this).divide(BigDecimal("100")).toDouble()
    }

    fun String.multiplyBy10AndConvertToDouble(): Double {
        return BigDecimal(this).multiply(BigDecimal("10")).toDouble()
    }

    @Throws(Exception::class)
    fun encrypt(content: String): String? {
        val cipher = Cipher.getInstance("AES/CFB/NoPadding")
        val secretKey: SecretKey = SecretKeySpec(SLAT_KEY.toByteArray(), "AES")
        val iv = IvParameterSpec(VECTOR_KEY.toByteArray())
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
        val encrypted = cipher.doFinal(content.toByteArray())
        return Base64.encodeToString(encrypted, Base64.DEFAULT)
    }

    fun formatTimestampToTime(timestamp: Timestamp?, showDate: Boolean = false): String {
        return timestamp?.let {
            val dateFormat = if (showDate) "dd/MM" else ""
            val sdf = SimpleDateFormat("$dateFormat HH:mm", Locale.getDefault())
            sdf.format(Date(it.seconds * 1000))
        } ?: "N/A"
    }

    fun isSameDay(startTime: Timestamp?, endTime: Timestamp?): Boolean {
        if (startTime == null || endTime == null) {
            return false
        }

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) // Only the date part
        val startDate = sdf.format(Date(startTime.seconds * 1000))
        val endDate = sdf.format(Date(endTime.seconds * 1000))

        return startDate == endDate
    }

    /*
        * Perform haptic feedback and play sound effect when a view is clicked
        * because compose does not do this by default
     */
    fun performVirtualTapFeedback(view: View) {
        view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        view.playSoundEffect(android.view.SoundEffectConstants.CLICK)
    }

    fun formatSecondsToCompactFormat(seconds: Long): String {
        val days = seconds / (24 * 3600)
        val hours = (seconds % (24 * 3600)) / 3600
        val minutes = (seconds % 3600) / 60

        val daysPart = if (days > 0) "${days}d " else ""
        val hoursPart = if (hours > 0) "${hours}h " else ""
        val minutesPart = if (minutes > 0) "${minutes}m" else ""
        val empty = "0m"

        return if (days > 0) {
            "$daysPart$hoursPart$minutesPart"
        } else if (hours > 0) {
            "$hoursPart$minutesPart"
        } else if (minutes > 0) {
            minutesPart
        } else {
            empty
        }
    }

    fun getFormattedStartPrice(startingPrice: String): String {
        return if (startingPrice.toDoubleOrNull() == null) {
            startingPrice
        } else {
            "$${String.format(Locale.US, "%.2f", (startingPrice).toDouble() / 100)}"
        }
    }

    fun getFormattedStepPrice(stepPrice: String): String {
        return if (stepPrice.toDoubleOrNull() == null) {
            stepPrice
        } else {
            "$${String.format(Locale.US, "%.2f", (stepPrice).toDouble() / 5 / 100)}"
        }
    }

    fun getFormattedChangedPriceAt(changedPriceAt: String): String {
        return if (changedPriceAt.toDoubleOrNull() == null) {
            changedPriceAt
        } else {
            "$${String.format(Locale.US, "%.2f", (changedPriceAt).toDouble() / 10)}"
        }
    }

    fun getFormattedChangedStepPrice(changedStepPrice: String): String {
        return if (changedStepPrice.toDoubleOrNull() == null) {
            changedStepPrice
        } else {
        "$${String.format(Locale.US, "%.2f", (changedStepPrice).toDouble() / 5 / 100)}"
            }
    }
}