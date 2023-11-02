package com.urielsalis.codecrafters.bittorent

import com.urielsalis.codecrafters.bittorent.bencode.BencodeParser

fun main(args: Array<String>) {
    val command = args[0]
    try {
        when (command) {
            "decode" -> return runDecodeCommand(args)
            else -> println("Unknown command $command")
        }
    } catch (e: RuntimeException) {
        e.printStackTrace()
    }
}

fun runDecodeCommand(args: Array<String>) {
    val bencodeParser = BencodeParser()
    val bencodedValue = args[1]
    val decoded = bencodeParser.parseNext(bencodedValue)
    println(decoded.toJson())
}
