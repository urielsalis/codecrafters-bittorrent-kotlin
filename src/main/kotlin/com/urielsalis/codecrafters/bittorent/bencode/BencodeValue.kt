package com.urielsalis.codecrafters.bittorent.bencode

import com.google.gson.Gson
import java.math.BigInteger
import java.nio.charset.Charset

val gson = Gson()
sealed interface BencodeValue {
    fun toJson(): String
}

class StringBencodeValue(val value: ByteArray) : BencodeValue {
    override fun toJson(): String = gson.toJson(asString())
    fun asString(): String = value.toString(Charset.defaultCharset())
}

fun ByteArray.toBencodeValue() = StringBencodeValue(this)
fun String.toBencodeValue() = StringBencodeValue(this.toByteArray())
