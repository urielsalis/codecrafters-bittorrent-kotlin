@file:OptIn(ExperimentalStdlibApi::class) // FIXME: remove when .toHexString() is no longer experimental

package com.urielsalis.codecrafters.bittorent

import com.urielsalis.codecrafters.bittorent.bencode.BencodeParser
import com.urielsalis.codecrafters.bittorent.magnet.MagnetParser
import com.urielsalis.codecrafters.bittorent.metainfo.MetaInfoParser
import com.urielsalis.codecrafters.bittorent.peer.ConnectionManager
import com.urielsalis.codecrafters.bittorent.peer.domain.Peer
import com.urielsalis.codecrafters.bittorent.peer.PeerConnection
import com.urielsalis.codecrafters.bittorent.peer.TrackerManager
import org.apache.commons.codec.binary.Hex
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
            "magnet_parse" -> return parseMagnet(args)
            "magnet_handshake" -> return magnetHandshake(args)
            "magnet_info" -> return runMagnetInfo(args)
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
    val connectionManager = ConnectionManager(metaInfo.infoHash, peers, metaInfo)
    connectionManager.markInterested()
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
    val connectionManager = ConnectionManager(metaInfo.infoHash, peers, metaInfo)
    connectionManager.markInterested()
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
    println("Peer ID: ${conn.handshake(metaInfo.infoHash).toHexString()}")
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

fun parseMagnet(args: Array<String>) {
    val magnet = args[1]
    val magnetInfo = MagnetParser.parse(magnet)
    println("Tracker URL: ${magnetInfo.trackerUrl}")
    println("Info Hash: ${magnetInfo.infoHash}")
}

fun magnetHandshake(args: Array<String>) {
    val magnet = args[1]
    val magnetInfo = MagnetParser.parse(magnet)
    val peers = TrackerManager.getPeers(magnetInfo)
    val connection = PeerConnection(peers.first())
    val peerId = connection.handshake(Hex.decodeHex(magnetInfo.infoHash))
    connection.initConnection()
    println("Peer ID: ${peerId.toHexString()}")
    println("Peer Metadata Extension ID: ${connection.metadataExtensionId}")
}

fun runMagnetInfo(args: Array<String>) {
    val magnet = args[1]
    val magnetInfo = MagnetParser.parse(magnet)
    val peers = TrackerManager.getPeers(magnetInfo)
    val connection = ConnectionManager(Hex.decodeHex(magnetInfo.infoHash), peers, null)
    // TODO print metadata
}

