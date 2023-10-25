package com.urielsalis.codecrafters.bittorent.bencode

import com.urielsalis.codecrafters.bittorent.InvalidDictionaryKeyException
import com.urielsalis.codecrafters.bittorent.ParserException
import com.urielsalis.codecrafters.bittorent.bencode.BencodeParser.ToBencode.decodeDictionary
import com.urielsalis.codecrafters.bittorent.bencode.BencodeParser.ToBencode.decodeInteger
import com.urielsalis.codecrafters.bittorent.bencode.BencodeParser.ToBencode.decodeList
import com.urielsalis.codecrafters.bittorent.bencode.BencodeParser.ToBencode.decodeString
import com.urielsalis.codecrafters.bittorent.bencode.BencodeParser.ToObject.parseDict
import com.urielsalis.codecrafters.bittorent.bencode.BencodeParser.ToObject.parseInteger
import com.urielsalis.codecrafters.bittorent.bencode.BencodeParser.ToObject.parseList
import com.urielsalis.codecrafters.bittorent.bencode.BencodeParser.ToObject.parseString
import com.urielsalis.codecrafters.bittorent.skip
import com.urielsalis.codecrafters.bittorent.toBigInteger
import java.math.BigInteger.TEN
import java.math.BigInteger.ZERO
import java.nio.charset.Charset

const val INTEGER_TAG = 'i'
const val LIST_TAG = 'l'
const val DICTIONARY_TAG = 'd'
const val END_STRING_TAG = ':'.code.toByte()
const val END_TAG_TAG = 'e'.code.toByte()
const val NEGATIVE_TAG = '-'.code.toByte()

object BencodeParser {

    fun toBencode(value: BencodeValue): ByteArray {
        return when (value) {
            is StringBencodeValue -> decodeString(value)
            is IntegerBencodeValue -> decodeInteger(value)
            is ListBencodeValue -> decodeList(value)
            is DictionaryBencodeValue -> decodeDictionary(value)
        }
    }

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

    object ToBencode {
        fun decodeString(value: StringBencodeValue) =
            value.value.size.toString().toByteArray() + END_STRING_TAG + value.value

        fun decodeInteger(value: IntegerBencodeValue) =
            byteArrayOf(INTEGER_TAG.code.toByte()) + value.value.toString(10)
                .toByteArray() + END_TAG_TAG

        fun decodeList(value: ListBencodeValue) =
            byteArrayOf(LIST_TAG.code.toByte()) + value.values.map { toBencode(it) }
                .reduce { acc, arr -> acc + arr } + END_TAG_TAG

        fun decodeDictionary(value: DictionaryBencodeValue) =
            byteArrayOf(DICTIONARY_TAG.code.toByte()) + value.values.map { (key, value) ->
                toBencode(key.toBencodeValue()) + toBencode(value)
            }.reduce { acc, arr -> acc + arr } + END_TAG_TAG
    }

    object ToObject {
        fun parseDict(bencodeValue: ByteArray): Pair<BencodeValue, ByteArray> {
            val values = mutableMapOf<ByteArray, BencodeValue>()
            var currentBencodeValue = bencodeValue.skip(1)
            while (currentBencodeValue[0] != END_TAG_TAG) {
                val (key, newBencodeValue) = parseNext(currentBencodeValue)
                if (key !is StringBencodeValue) {
                    throw InvalidDictionaryKeyException(key)
                }
                val (value, newBencodeValue2) = parseNext(newBencodeValue)
                values[key.value] = value
                currentBencodeValue = newBencodeValue2
            }
            return values.toBencodeValue() to currentBencodeValue.skip(1)
        }

        fun parseList(bencodeValue: ByteArray): Pair<ListBencodeValue, ByteArray> {
            val values = mutableListOf<BencodeValue>()
            var currentBencodeValue = bencodeValue.skip(1)
            while (currentBencodeValue[0] != END_TAG_TAG) {
                val (valueToAdd, newBencodeValue) = parseNext(currentBencodeValue)
                values.add(valueToAdd)
                currentBencodeValue = newBencodeValue
            }
            return values.toBencodeValue() to currentBencodeValue.skip(1)
        }

        fun parseInteger(bencodeValue: ByteArray): Pair<IntegerBencodeValue, ByteArray> {
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

        fun parseString(bencodeValue: ByteArray): Pair<StringBencodeValue, ByteArray> {
            val index = bencodeValue.indexOfFirst { it == END_STRING_TAG }
            val length = bencodeValue.copyOf(index).toString(Charset.defaultCharset()).toInt(10)
            val totalLength = index + 1 + length
            val value = bencodeValue.copyOfRange(index + 1, totalLength).toBencodeValue()
            return value to bencodeValue.skip(totalLength)
        }
    }
}
