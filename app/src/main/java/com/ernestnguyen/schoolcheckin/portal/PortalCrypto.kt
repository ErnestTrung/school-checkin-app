package com.ernestnguyen.schoolcheckin.portal

import java.util.Base64

/**
 * Port of the portal's `AE`/`AD` helpers (appended to its crypto-js.js): a repeating-key XOR
 * over UTF-16 code units, carried as Base64 of the UTF-8 bytes.
 *
 * Requests whose payload contains `iM` are sent as `A=AE(json, <action text after '/'>)`;
 * responses whose `Data` is `{B: ...}` are read with `AD(B, iM)`.
 */
object PortalCrypto {
    /** `edu.system.iM`, hard-coded by the portal's `AFG()`; also the response key. */
    const val IM = "AzzSystem"

    fun encrypt(plain: String, key: String): String =
        Base64.getEncoder().encodeToString(xor(plain, key).toByteArray(Charsets.UTF_8))

    fun decrypt(encoded: String, key: String): String =
        xor(String(Base64.getMimeDecoder().decode(encoded), Charsets.UTF_8), key)

    private fun xor(text: String, key: String): String {
        val out = CharArray(text.length)
        for (i in text.indices) {
            out[i] = (text[i].code xor key[i % key.length].code).toChar()
        }
        return String(out)
    }
}
