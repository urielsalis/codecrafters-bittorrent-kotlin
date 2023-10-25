@file:OptIn(ExperimentalStdlibApi::class) // FIXME: remove when .toHexString() is no longer experimental

package com.urielsalis.codecrafters.bittorent

import com.urielsalis.codecrafters.bittorent.bencode.BencodeParser
import com.urielsalis.codecrafters.bittorent.metainfo.MetaInfoParser

fun main(args: Array<String>) {
    val command = args[0]
    try {
        when (command) {
            "decode" -> return runDecodeCommand(args)
            "info" -> return runInfoCommand(args)
            else -> println("Unknown command $command")
        }
    } catch (e: RuntimeException) {
        e.printStackTrace()
    }
}

fun runInfoCommand(args: Array<String>) {
    val metainfoFile = args[1]
    val metaInfo = MetaInfoParser.parse(metainfoFile)
    println("Tracker URL: ${metaInfo.announce}")
    println("Length: ${metaInfo.fileLength}")
    println("Info Hash: ${metaInfo.infoHash.toHexString()}")
}

fun runDecodeCommand(args: Array<String>) {
    val bencodedValue = args[1]
    val decoded = BencodeParser.parseNext(bencodedValue).first
    println(decoded.toJson())
}
