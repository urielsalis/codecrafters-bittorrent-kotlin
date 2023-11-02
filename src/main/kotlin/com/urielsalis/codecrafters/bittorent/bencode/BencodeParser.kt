package com.urielsalis.codecrafters.bittorent.bencode

import com.urielsalis.codecrafters.bittorent.ParserException
import java.nio.charset.Charset

const val END_STRING_TAG = ':'.code.toByte()
const val END_TAG_TAG = 'e'.code.toByte()
const val NEGATIVE_TAG = '-'.code.toByte()

class BencodeParser {

    fun parseNext(bencodeValue: String): BencodeValue = parseNext(bencodeValue.toByteArray())
    fun parseNext(bencodeValue: ByteArray): BencodeValue {
        return when (bencodeValue[0].toInt().toChar()) {
            in '1'..'9' -> parseString(bencodeValue)
            else -> throw ParserException("Unknown type ${bencodeValue[0]}")
        }
    }

}

private fun parseString(bencodeValue: ByteArray): StringBencodeValue {
    val index = bencodeValue.indexOfFirst { it == END_STRING_TAG }
    val length = bencodeValue.copyOf(index).toString(Charset.defaultCharset()).toInt(10)
    return bencodeValue.copyOfRange(index + 1, index + 1 + length).toBencodeValue()
}

