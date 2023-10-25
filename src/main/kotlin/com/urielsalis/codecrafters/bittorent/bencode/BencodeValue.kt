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

class IntegerBencodeValue(val value: BigInteger) : BencodeValue {
    override fun toJson(): String = gson.toJson(value)
    fun asInt(): Int = value.toInt()
}

class ListBencodeValue(val values: List<BencodeValue>) : BencodeValue {
    override fun toJson(): String = '[' + values.joinToString(",") { it.toJson() } + ']'
}

class DictionaryBencodeValue(val values: Map<ByteArray, BencodeValue>) : BencodeValue {
    override fun toJson(): String =
        '{' + values.map { "\"${it.key.toString(Charset.defaultCharset())}\":${it.value.toJson()}" }
            .joinToString(",") + '}'
}

fun ByteArray.toBencodeValue() = StringBencodeValue(this)
fun String.toBencodeValue() = StringBencodeValue(this.toByteArray())
fun BigInteger.toBencodeValue() = IntegerBencodeValue(this)
fun Int.toBencodeValue() = IntegerBencodeValue(this.toBigInteger())
fun List<BencodeValue>.toBencodeValue() = ListBencodeValue(this)
fun Map<ByteArray, BencodeValue>.toBencodeValue() = DictionaryBencodeValue(this)
