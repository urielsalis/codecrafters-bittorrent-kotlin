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
    fun asLong(): Long = value.toLong()
}

class ListBencodeValue(val values: List<BencodeValue>) : BencodeValue {
    override fun toJson(): String = '[' + values.joinToString(",") { it.toJson() } + ']'
}

class DictionaryBencodeValue(val values: Map<ByteArray, BencodeValue>) : BencodeValue {
    override fun toJson(): String =
        '{' + values.map { "\"${it.key.toString(Charset.defaultCharset())}\":${it.value.toJson()}" }
            .joinToString(",") + '}'

    // FIXME: ByteArray doesn't have equals so its annoying to use as key.
    //  Change to a value class once they support overriding equals
    operator fun get(key: ByteArray): BencodeValue? =
        values.entries.firstOrNull { it.key.contentEquals(key) }?.value

    operator fun get(key: String): BencodeValue? = get(key.toByteArray())
}

fun ByteArray.toBencodeValue() = StringBencodeValue(this)
fun String.toBencodeValue() = StringBencodeValue(this.toByteArray())
fun BigInteger.toBencodeValue() = IntegerBencodeValue(this)
fun Int.toBencodeValue() = IntegerBencodeValue(this.toBigInteger())
fun List<BencodeValue>.toBencodeValue() = ListBencodeValue(this)
fun Map<ByteArray, BencodeValue>.toBencodeValue() = DictionaryBencodeValue(this)
