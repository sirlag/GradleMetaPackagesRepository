package com.github.sirlag.Gravatar

import java.security.MessageDigest

class Gravatar(private val email: String){
    fun getGravatarHash() : String{
        val messageDigest = MessageDigest.getInstance("MD5")
        messageDigest.update(email.toByteArray())
        val sb = StringBuilder()
        messageDigest.digest().forEach {
            sb.append(Character.forDigit(it.toInt() and 0xf0 shr 4, 16))
            sb.append(Character.forDigit(it.toInt() and 0x0f, 16))
        }
        return sb.toString()
    }

    fun getGravatarHash(size: Int): String{
        return getGravatarHash() + "?s=${size}"
    }
}