package com.cutm.nt14.domain.detector

import java.security.MessageDigest

object FingerprintGenerator {

    fun generateHash(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun generateUserFingerprint(userId: String, ip: String, deviceId: String): String {
        return generateHash("$userId|$ip|$deviceId")
    }
}
