package com.urielsalis.codecrafters.bittorent

import com.urielsalis.codecrafters.bittorent.bencode.BencodeValue


open class ParserException(message: String) : Exception(message)
class InvalidDictionaryKey(bencodeValue: BencodeValue) :
    ParserException("Invalid dictionary key type. Expected String, got $bencodeValue")