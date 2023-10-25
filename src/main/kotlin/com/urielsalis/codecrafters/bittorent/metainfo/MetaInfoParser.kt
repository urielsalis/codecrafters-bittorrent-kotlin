package com.urielsalis.codecrafters.bittorent.metainfo

import com.urielsalis.codecrafters.bittorent.InvalidMetaInfoFileException
import com.urielsalis.codecrafters.bittorent.MismatchedTypeException
import com.urielsalis.codecrafters.bittorent.ParserException
import com.urielsalis.codecrafters.bittorent.asType
import com.urielsalis.codecrafters.bittorent.bencode.*
import java.io.File
import java.security.MessageDigest

object MetaInfoParser {
    fun parse(filename: String): MetaInfo {
        val parsedDict = BencodeParser.parseNext(File(filename).readBytes()).first
        if (parsedDict !is DictionaryBencodeValue) {
            throw InvalidMetaInfoFileException(parsedDict)
        }
        val announce = parsedDict["announce"]?.asType<StringBencodeValue>()
            ?: throw ParserException("'announce' not found inside metainfo")
        val infoDict = parsedDict["info"]?.asType<DictionaryBencodeValue>()
            ?: throw ParserException("'info' not found inside metainfo")
        val piecesStr = infoDict["pieces"]?.asType<StringBencodeValue>()
            ?: throw ParserException("'pieces' not found inside info")
        val fileLength = infoDict["length"]?.asType<IntegerBencodeValue>() ?: throw ParserException(
            "'length' not found inside info"
        )
        val fileName = infoDict["name"]?.asType<StringBencodeValue>()
            ?: throw ParserException("'length' not found inside info")
        val pieceLength = infoDict["piece length"]?.asType<IntegerBencodeValue>()
            ?: throw ParserException("'length' not found inside info")

        val infoHash = MessageDigest.getInstance("SHA-1").digest(BencodeParser.toBencode(infoDict))
        val pieces = piecesStr.value.toList().chunked(20) { it.toByteArray() }
        return MetaInfo(
            announce.asString(),
            infoHash,
            fileLength.asLong(),
            fileName.asString(),
            pieceLength.asInt(),
            pieces
        )
    }
}
