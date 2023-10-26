@file:OptIn(ExperimentalStdlibApi::class) // FIXME: remove when .toHexString() is no longer experimental

package com.urielsalis.codecrafters.bittorent

import com.urielsalis.codecrafters.bittorent.bencode.BencodeParser
import com.urielsalis.codecrafters.bittorent.metainfo.MetaInfoParser
import com.urielsalis.codecrafters.bittorent.peer.ConnectionManager
import com.urielsalis.codecrafters.bittorent.peer.domain.Peer
import com.urielsalis.codecrafters.bittorent.peer.PeerConnection
import com.urielsalis.codecrafters.bittorent.peer.TrackerManager
import java.io.File

fun main(args: Array<String>) {
    val command = args[0]
    try {
        when (command) {
            "decode" -> return runDecodeCommand(args)
            "info" -> return runInfoCommand(args)
            "peers" -> return runPeersCommand(args)
            "handshake" -> return runHandshakeCommand(args)
            "download_piece" -> return runDownloadPiece(args)
            "download" -> return runDownload(args)
            else -> println("Unknown command $command")
        }
    } catch (e: RuntimeException) {
        e.printStackTrace()
    }
}

fun runDownload(args: Array<String>) {
    val outputFilename = args[2]
    val metaInfoFile = args[3]
    val metaInfo = MetaInfoParser.parse(metaInfoFile)
    val peers = TrackerManager.getPeers(metaInfo)
    val connectionManager = ConnectionManager(metaInfo, peers)
    connectionManager.connect()
    connectionManager.requestAllPieces()
    connectionManager.download()
    connectionManager.writeToFile(outputFilename)
    println("Downloaded $metaInfoFile to $outputFilename.")
}

fun runDownloadPiece(args: Array<String>) {
    val outputFilename = args[2]
    val metaInfoFile = args[3]
    val pieceNumber = args[4].toInt()
    val metaInfo = MetaInfoParser.parse(metaInfoFile)
    val peers = TrackerManager.getPeers(metaInfo)
    val connectionManager = ConnectionManager(metaInfo, peers)
    connectionManager.connect()
    connectionManager.requestPiece(pieceNumber)
    connectionManager.download()
    val piece = connectionManager.getPiece(pieceNumber)
    val file = File(outputFilename)
    file.writeBytes(piece)
    println("Piece $pieceNumber downloaded to $outputFilename.")
}

fun runHandshakeCommand(args: Array<String>) {
    val metainfoFile = args[1]
    val peer = args[2]
    val metaInfo = MetaInfoParser.parse(metainfoFile)
    val conn = PeerConnection(Peer(peer))
    println("Peer ID: ${conn.handshake(metaInfo).toHexString()}")
}

fun runPeersCommand(args: Array<String>) {
    val metainfoFile = args[1]
    val metaInfo = MetaInfoParser.parse(metainfoFile)
    val peers = TrackerManager.getPeers(metaInfo)
    peers.forEach { println("${it.socket.hostString}:${it.socket.port}") }
}

fun runInfoCommand(args: Array<String>) {
    val metainfoFile = args[1]
    val metaInfo = MetaInfoParser.parse(metainfoFile)
    println("Tracker URL: ${metaInfo.announce}")
    println("Length: ${metaInfo.fileLength}")
    println("Info Hash: ${metaInfo.infoHash.toHexString()}")
    println("Piece Length: ${metaInfo.pieceLength}")
    println("Piece Hashes:")
    metaInfo.pieces.forEach { println(it.toHexString()) }
}

fun runDecodeCommand(args: Array<String>) {
    val bencodedValue = args[1]
    val decoded = BencodeParser.parseNext(bencodedValue).first
    println(decoded.toJson())
}
