package com.vismo.cablemeter.util

import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import android.util.Base64
import com.google.firebase.Timestamp
import com.vismo.cablemeter.util.Constant.SLAT_KEY
import com.vismo.cablemeter.util.Constant.VECTOR_KEY
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
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

    fun formatTimestampToTime(timestamp: Timestamp?): String {
        return timestamp?.let {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            sdf.format(Date(it.seconds * 1000))
        } ?: "N/A"
    }
}