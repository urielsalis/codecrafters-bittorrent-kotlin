package com.urielsalis.codecrafters.bittorent

import com.urielsalis.codecrafters.bittorent.bencode.BencodeValue
import kotlin.reflect.KClass


open class ParserException(message: String) : Exception(message)
class InvalidDictionaryKeyException(bencodeValue: BencodeValue) :
    ParserException("Invalid dictionary key type. Expected String, got $bencodeValue")

class InvalidMetaInfoFileException(bencodeValue: BencodeValue) :
    ParserException("Invalid metainfo file. Expected root to be dictionary, got $bencodeValue")

class MismatchedTypeException(expected: KClass<out BencodeValue>, got: BencodeValue) : ParserException("Expected type ${expected.simpleName}, got $got")