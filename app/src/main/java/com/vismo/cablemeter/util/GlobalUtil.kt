package com.vismo.cablemeter.util

import java.security.KeyFactory
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import android.util.Base64

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
}