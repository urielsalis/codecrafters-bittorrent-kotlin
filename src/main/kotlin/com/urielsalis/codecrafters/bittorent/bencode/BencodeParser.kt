package com.urielsalis.codecrafters.bittorent.bencode

import com.urielsalis.codecrafters.bittorent.InvalidDictionaryKey
import com.urielsalis.codecrafters.bittorent.ParserException
import com.urielsalis.codecrafters.bittorent.skip
import com.urielsalis.codecrafters.bittorent.toBigInteger
import sun.security.pkcs.ParsingException
import java.math.BigInteger.TEN
import java.math.BigInteger.ZERO
import java.nio.charset.Charset

const val INTEGER_TAG = 'i'
const val LIST_TAG = 'l'
const val DICTIONARY_TAG = 'd'
const val END_STRING_TAG = ':'.code.toByte()
const val END_TAG_TAG = 'e'.code.toByte()
const val NEGATIVE_TAG = '-'.code.toByte()

class BencodeParser {

    fun parseNext(bencodeValue: String): Pair<BencodeValue, ByteArray> =
        parseNext(bencodeValue.toByteArray())

    fun parseNext(bencodeValue: ByteArray): Pair<BencodeValue, ByteArray> {
        return when (bencodeValue[0].toInt().toChar()) {
            in '1'..'9' -> parseString(bencodeValue)
            INTEGER_TAG -> parseInteger(bencodeValue)
            LIST_TAG -> parseList(bencodeValue)
            DICTIONARY_TAG -> parseDict(bencodeValue)
            else -> throw ParserException("Unknown type ${bencodeValue[0]}")
        }
    }

    private fun parseDict(bencodeValue: ByteArray): Pair<BencodeValue, ByteArray> {
        val values = mutableMapOf<ByteArray, BencodeValue>()
        var currentBencodeValue = bencodeValue.skip(1)
        while (currentBencodeValue[0] != END_TAG_TAG) {
            val (key, newBencodeValue) = parseNext(currentBencodeValue)
            if (key !is StringBencodeValue) {
                throw InvalidDictionaryKey(key)
            }
            val (value, newBencodeValue2) = parseNext(newBencodeValue)
            values[key.value] = value
            currentBencodeValue = newBencodeValue2
        }
        return values.toBencodeValue() to currentBencodeValue.skip(1)
    }

    private fun parseList(bencodeValue: ByteArray): Pair<ListBencodeValue, ByteArray> {
        val values = mutableListOf<BencodeValue>()
        var currentBencodeValue = bencodeValue.skip(1)
        while (currentBencodeValue[0] != END_TAG_TAG) {
            val (valueToAdd, newBencodeValue) = parseNext(currentBencodeValue)
            values.add(valueToAdd)
            currentBencodeValue = newBencodeValue
        }
        return values.toBencodeValue() to currentBencodeValue.skip(1)
    }

    private fun parseInteger(bencodeValue: ByteArray): Pair<IntegerBencodeValue, ByteArray> {
        val isNegative = bencodeValue[1] == NEGATIVE_TAG
        val value = bencodeValue.asSequence().drop(1).takeWhile { it != END_TAG_TAG }.map {
            when (it) {
                NEGATIVE_TAG -> ZERO
                else -> it.toBigInteger()
            }
        }.reduce { acc, cur -> acc.multiply(TEN).add(cur) }
        if (isNegative) {
            return value.negate()
                .toBencodeValue() to bencodeValue.skip(value.toString(10).length + 3)
        }
        return value.toBencodeValue() to bencodeValue.skip(value.toString(10).length + 2)
    }

    private fun parseString(bencodeValue: ByteArray): Pair<StringBencodeValue, ByteArray> {
        val index = bencodeValue.indexOfFirst { it == END_STRING_TAG }
        val length = bencodeValue.copyOf(index).toString(Charset.defaultCharset()).toInt(10)
        val totalLength = index + 1 + length
        val value = bencodeValue.copyOfRange(index + 1, totalLength).toBencodeValue()
        return value to bencodeValue.skip(totalLength)
    }
}
